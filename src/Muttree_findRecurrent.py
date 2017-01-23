#!/usr/bin/env python

#
# MUTTREE: A PIPELINE FOR PHYLOGENETIC TREE RECONSTRUCTION AND RECURRENT MUTATION DISCOVERY
#
# Adrian Baez-Ortega (ab2324@cam.ac.uk)
# Transmissible Cancer Group, University of Cambridge
# 2016
#

# Muttree_findRecurrent.py: Remaps position of output mutations from treesub into position in the input
#                           gene sequences, using an input 'gene table'. After this, identifies recurrent
#                           mutations, defined as non-synonymous mutations occurring in a same gene.

# INPUT
#   geneTable: tab-delimited text file with two columns: gene name and start position
#   treesubDir: directory where the treesub output is
#   outDir: output directory (must exist)


from __future__ import division
import sys
import os
import re
import math


# If not 3 arguments: print help
if len(sys.argv) != 5:
    print '\nMuttree_findRecurrent.py: Remaps the position of the output mutations from treesub into'
    print '                          their position in the input gene CDS sequences, using a \'gene table\'.'
    print '                          After this, identifies recurrent mutations, defined as non-synonymous'
    print '                          mutations affecting a same gene in different branches.'
    print '                   Input: Path to the gene table, defined as a tab-delimited text file with two'
    print '                            columns: gene name and start position'
    print '                          Path to the directory where the treesub output (.tsv and .tree files) is'
    print '                          Path to file containing the positions of variable codons in the original sequence'
    print '                          Path to output directory'
    print '                   Usage: Muttree_findRecurrent.py /path/to/gene_table.txt /path/to/in_dir path/to/codon_pos.txt /path/to/out_dir\n'
    sys.exit(0)


script, geneTable, treesubDir, positionsFile, outDir = sys.argv
mutTable = treesubDir + '/substitutions.tsv'
mutTree = treesubDir + '/substitutions.tree'
outTable = outDir + '/Muttree_Substitutions.tsv'
outTree = outDir + '/Muttree_Substitutions_All.nexus.tree'
recTree = outDir + '/Muttree_Substitutions_Recurrent.nexus.tree'


# 1. Read gene table and variable codon positions
print '\nRunning Muttree_findRecurrent.py'
print '\nReading gene table from: ' + geneTable

geneNames = []
geneStart = []
cnt = 0

with open(geneTable, 'r') as table:
    for line in table:
        cnt = cnt + 1
        geneNames.append(line.strip().split('\t')[0])
        geneStart.append(int(line.strip().split('\t')[1]))

# Sort by start position
geneNames = [x for (y,x) in sorted(zip(geneStart,geneNames))]
geneStart = sorted(geneStart)

print str(cnt) + ' entries read'

print '\nReading variable codon positions from: ' + positionsFile
with open(positionsFile, 'r') as pos:
    codonPos = pos.readline().strip().split(' ')



# 2. Remap position of mutations in the output treesub table (substitutions.tsv)
print '\nRemapping mutation positions from table: ' + mutTable

remapped = {}       # Remapped coordinates per mutation
nonSynPerGene = {}  # Number of different non-synonymous mutations per gene
newLines = []       # Output data
cnt = 0
mutIdx = 1

with open(mutTable, 'r') as table:
    # Skip header line
    next(table)
    
    for line in table:
        cnt = cnt + 1
        col = line.strip().split('\t')
        [branch, site, codonFrom, codonTo, aaFrom, aaTo, string] = col[0:7]
        if len(col) > 7:
            nonSyn = 'Y'
        else:
            nonSyn = 'N'
        
        # Retrieve codon position in the original sequence
        realSite = int(codonPos[int(site)])  # 1-based
        
        # Look up the affected gene in the gene table
        for i in list(reversed(range(len(geneStart)))):
            aaStart = int(math.ceil(geneStart[i]/3))
            if realSite >= aaStart:
                break
        
        newSite = realSite - aaStart + 1
        gene = geneNames[i]
        newString = gene + '_' + aaFrom + str(newSite) + aaTo
        
        # Create mutation index for the tree
        index = 'M' + str(mutIdx)
        
        # Record gene and index
        remapped[string] = gene + '_' + index
        newLines.append([index, branch, gene, str(newSite), codonFrom, codonTo, aaFrom, aaTo, newString, nonSyn])
        
        # If non-synonymous, update nonSynPerGene
        if nonSyn == 'Y':
            if gene in nonSynPerGene:
                nonSynPerGene[gene] = nonSynPerGene[gene] + 1
            else:
                nonSynPerGene[gene] = 1
        
        mutIdx = mutIdx + 1
        
    # Sort by mutation position
    #newLines = [x for (y,x) in sorted(zip(sites,newLines))]
    
    print str(cnt) + ' mutations remapped'        



# 3. Identify recurrent mutations and write new mutations table
# Get gene names with >1 non-synonymous mutation
recGenes = [k for k, v in nonSynPerGene.items() if v > 1]
print 'Recurrent mutations identified in genes: ' + ', '.join(recGenes)

# Write newLines to output file
print '\nWriting remapped mutations to: ' + outTable + '\n'

recMutations = []
with open(outTable, 'w') as out:
    out.write('Index	Branch	Gene	Site	Codon_from	Codon_to	AA_from	AA_to	String	Nonsynonymous	Recurrent\n')
    for line in newLines:
        # If mutation is non-syn and gene is in list of recurrent genes: output as recurrent
        if line[9] == 'Y' and line[1] in recGenes:
            recurrent = 'Y'
            recMutations.append(line[7])
        else:
            recurrent = 'N'
        line.append(recurrent)
        out.write('\t'.join(line) + '\n')



# 4. Remap substitutions in treesub tree and generate recurrent mutations tree
print 'Remapping mutation positions from tree: ' + mutTree
print 'Writing remapped tree to: ' + outTree
print 'Writing recurrent mutations tree to: ' + recTree

with open(mutTree, 'r') as tree, open(outTree, 'w') as out, open(recTree, 'w') as outRec:
    for line in tree:
        if not line.startswith('\t\t') and not line.startswith('tree'):
            out.write(line)
            outRec.write(line)
        else:
        
            # Replace each substitution with its remapped version
            outLine = line
            recLine = line
            for mut in remapped:
                outLine = outLine.replace(mut, remapped[mut])
                # For the recurrent mutations tree: if the mutation is not recurrent, remove it
                if remapped[mut] in recMutations:
                    recLine = recLine.replace(mut, remapped[mut])
                else:
                    recLine = recLine.replace(mut + ', ', '').replace(', ' + mut, '').replace(mut, '')
            
            # Output new lines
            out.write(outLine)
            outRec.write(recLine)


print '\nDone\n'

