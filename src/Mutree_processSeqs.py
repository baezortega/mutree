#!/usr/bin/env python

#
# MUTREE: A PIPELINE FOR PHYLOGENETIC TREE INFERENCE AND RECURRENT MUTATION DISCOVERY
#
# Adrian Baez-Ortega (ab2324@cam.ac.uk)
# Transmissible Cancer Group, University of Cambridge
# 2016
#

# Mutree_processSeqs.py: Transforms the input FASTA file of coding sequences to PHYLIP format, and generates
#                        a second PHYLIP file including only the codons that vary in any of the sequences,
#                        as well as a file with the positions of such codons in the original sequence.

# INPUT
#   sequenceFile: FASTA file of sequences
#   outDir: output directory (must exist)


from __future__ import division
import sys
import os
import re
import math
from sets import Set


# If not 2 arguments: print help
if len(sys.argv) != 3:
    print '\nMutree_processSeqs.py: Transforms the input FASTA file of coding sequences to PHYLIP format, renaming'
    print '                       the sequences for compatibility with the rest of the pipeline.'
    print '                       Also generates a PHYLIP file including only the codons that vary in any of the'
    print '                       sequences, and a file with the positions of those codons in the original sequence.'
    print '                Input: Path to the FASTA file of sequences'
    print '                       Path to output directory'
    print '                Usage: Mutree_processSeqs.py /path/to/seqs.fasta /path/to/out_dir\n'
    sys.exit(0)


script, sequenceFile, outDir = sys.argv


print '\nRunning Mutree_processSeqs.py'
print '\nInput file:       ' + sequenceFile
print 'Output directory: ' + outDir

outSeqsFile = outDir + '/alignment_seqs.phylip'
outNamesFile = outDir + '/alignment_names'
outCodonsFile = outDir + '/alignment_codons.phylip'
outPositionsFile = outDir + '/codon_positions'


# 1. Obtain number of sequences, sequence length and variable sites
with open(sequenceFile, 'r') as seqs:
    count = 1
    numSeqs = 0
    varCodons = Set([])
    for line in seqs:
        seq = line.strip()
        
        # Even lines contain sequences
        if count % 2 == 0:
            numSeqs = numSeqs + 1
            
            # First sequence: get reference sequence and seq length
            if numSeqs == 1:
                seqLen = len(seq)
                refSeq = seq
            # Rest of sequences: get variable sites
            else:
                for i in range(0, seqLen):
                    if seq[i] != refSeq[i]:
                        varCodons.add(int(math.ceil((i + 1) / 3)))  # codon number, 1-based
        count = count + 1
        
    varCodons = sorted(varCodons)
    codLen = len(varCodons) * 3


# 2. Generate two PHYLIP files, one with whole sequences and one with only variable sites
print '\nWriting sequence names to:                   ' + outNamesFile
print 'Writing sequences in PHYLIP format to:       ' + outSeqsFile
print 'Writing variable codons in PHYLIP format to: ' + outCodonsFile
with open(sequenceFile, 'r') as seqs, open(outSeqsFile, 'w') as outSeqs, \
     open(outNamesFile, 'w') as outNames, open(outCodonsFile, 'w') as outCodons:
    
    # Write file headers
    outSeqs.write(str(numSeqs) + ' ' + str(seqLen) + '\n')
    outCodons.write(str(numSeqs) + ' ' + str(codLen) + '\n')
    
    count = 1
    idx = 1
    for line in seqs:
        seq = line.strip()
        
        # Odd lines contain sequence names
        if count % 2 != 0:
            outNames.write(seq[1:] + '\n')
            
        # Even lines contain sequences
        else:
            # Output sequence in PHYLIP format
            outSeqs.write('seq_' + str(idx) + '    ' + seq + '\n')
            
            # Output concatenated variable codons in PHYLIP format
            # Replace any 'N's by 'A's
            codons = ''.join(seq[x*3-3:x*3] for x in varCodons)
            codons = codons.replace('N', 'A')
            outCodons.write('seq_' + str(idx) + '    ' + codons + '\n')
            
            idx = idx + 1
        count = count + 1


# 3. Output variable site positions
print 'Writing variable codon positions to:         ' + outPositionsFile
with open(outPositionsFile, 'w') as outPositions:
    outPositions.write(' '.join(str(x) for x in varCodons) + '\n')

print '\nDone\n'
