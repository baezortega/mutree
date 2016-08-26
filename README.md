#### Download the latest release: muttree 1.0 ([zip](../../archive/v1.0.zip) | [tar.gz](../../archive/v1.0.tar.gz)).

---


muttree
=======

### A pipeline for phylogenetic tree construction and identification of recurrent mutations

Adrian Baez-Ortega
Transmissible Cancer Group, University of Cambridge

Muttree is a generalization and extension of [Asif Tamuri's treesub](https://github.com/tamuri/treesub) pipeline, which makes use of the tools [RAxML](http://sco.h-its.org/exelixis/web/software/raxml/index.html) [1], [PAML](http://abacus.gene.ucl.ac.uk/software/paml.html) [2] and parts of treesub itself (which in turns uses the Java libraries [PAL](http://iubio.bio.indiana.edu/soft/molbio/evolve/pal/pal.html) [3] and [BioJava](http://biojava.org/) [4]) in order to construct a phylogenetic tree and identify recurrent mutations in it, from nucleotide sequence alignment data.

The pipeline generates:

* A maximum likelihood phylogenetic tree (and, if bootstrapping is performed, a version including bootstrap values in its branches).

* A table with all the single-nucleotide substitutions found in the alignments, indicating whether they are non-synonymous and recurrent.

* A version of the tree showing all the annotated mutations in the branches where they occur.

* A version of the tree showing only the recurrent mutations in the branches where they occur.

The output tree files can be opened with using [Figtree](http://tree.bio.ed.ac.uk/software/figtree/) (although they may be compatible with other tools).

Muttree has been tested on an Ubuntu 14.04.4 system, and it should behave well in any Linux distribution. It has not been tested on Mac or Windows systems, but it might work with an appropriate bash shell.


---


## Installation

Muttree depends on the installation of the following software:

* __RAxML__, which can be downloaded from [Alexis Stamatakis's lab website](http://sco.h-its.org/exelixis/web/software/raxml/index.html). Muttree requires compiling the `raxmlHPC-SSE3` and `raxmlHPC-PTHREADS-SSE3` RAxML executables, which should work well in processors less than 5 years old.

* __PAML__, which can be downloaded from [Ziheng Yang's website](http://abacus.gene.ucl.ac.uk/software/paml.html).

* A recent __Java__ runtime (1.6+), which can be downloaded from the [Java website](https://www.java.com), although it is probably already installed in your system.

* __Figtree__, which can be downloaded from [Andrew Rambaut's website](http://tree.bio.ed.ac.uk/software/figtree/), is the recommended way of visualising the output trees.

Muttree already includes its own (slightly altered) version of the treesub pipeline, in the folder 'treesub-TCG'. Therefore, installation of treesub is not necessary, although it may be needed to re-compile it (see below).

The following instructions describe the steps for installing muttree and all its components in an Ubuntu 14.04.4 system; they should be valid for any Ubuntu or Debian Linux distribution. All of these tools have available Mac and Windows versions (please consult their respective websites). Muttree itself has not been tested on Mac or Windows systems, but it might work with an appropriate bash shell.

1. __Install RAxML__

You only need to install RAxML if the commands `which raxmlHPC-PTHREADS-SSE3` or `which raxmlHPC-SSE3` do not print anything in the terminal.

Go to the desired installation folder (in this example, the Software folder inside your home directory, or ~/Software):

    cd ~/Software

Download and compile RAxML:

    wget https://github.com/stamatak/standard-RAxML/archive/v8.2.9.tar.gz
    tar zxvf v8.2.9.tar.gz
    rm v8.2.9.tar.gz

    cd standard-RAxML-8.2.9/
    make -f Makefile.SSE3.gcc
    rm *.o
    make -f Makefile.SSE3.PTHREADS.gcc
    rm *.o

Edit your ~/.bashrc file using:

    nano ~/.bashrc

and append the standard-RAxML-8.2.9 directory at the end of your PATH variable. If the PATH variable is not defined, you can define it by adding the following line at the end of the .bashrc file:

    export PATH=~/Software/standard-RAxML-8.2.9:$PATH

Then save and close the file (Ctrl-X).

2. __Install PAML__

You only need to install PAML if the command `which baseml` does not print anything in the terminal.

Go to the desired installation folder, and download and compile PAML:

    cd ~/Software

    wget http://abacus.gene.ucl.ac.uk/software/paml4.9b.tgz
    tar zxvf paml4.9b.tgz
    rm paml4.9b.tgz

    cd paml4.9b/
    rm bin/*.exe
    cd src
    make -f Makefile
    rm *.o
    mv baseml basemlg codeml pamp evolver yn00 chi2 ../bin

Edit your ~/.bashrc file using:

    nano ~/.bashrc

and append the paml4.9b/bin directory at the end of your PATH variable. If the PATH variable was not defined before, now its line should look like:

    export PATH=~/Software/standard-RAxML-8.2.9:~/Software/paml4.9b/bin:$PATH

Then save and close the file (Ctrl-X).

3. __Install the Java Runtime Environment__

You only need to install Java if the command `which java` does not print anything in the terminal.

    sudo apt-get install default-jre

The system will ask for your password; you need to have administrator permissions in your system in order to use `sudo apt-get install`.

4. __Install FigTree__

You only need to install Java if the command `which figtree` does not print anything in the terminal.

    sudo apt-get install figtree

The system will ask for your password; you need to have administrator permissions in your system in order to use `sudo apt-get install`.

5. __Install muttree_

Go to the desired installation folder, and download the muttree repository:

    cd ~/Software

    wget https://github.com/adrianbaezortega/muttree/archive/v1.0.tar.gz
    tar zxvf v1.0.tar.gz
    rm v1.0.tar.gz

Edit your ~/.bashrc file using:

    nano ~/.bashrc

and append the muttree-1.0/src directory at the end of your PATH variable. If the PATH variable was not defined, not its line should look like:

    export PATH=~/Software/standard-RAxML-8.2.9:~/Software/paml4.9b/bin:~/Software/muttree-1.0/src:$PATH

Then save and close the file (Ctrl-X).

Either close the terminal and open a new one, or source the .bashrc file in order to apply the changes:

    source ~/.bashrc

Then you should be able to run the following commands, which should print something like these:

    which raxmlHPC-PTHREADS-SSE3  # prints: [...]/standard-RAxML-8.2.9/raxmlHPC-PTHREADS-SSE3
    which raxmlHPC-SSE3           # prints: [...]/standard-RAxML-8.2.9/which raxmlHPC-SSE3
    which baseml                  # prints: [...]/paml4.9b/bin/baseml
    which java                    # prints: /usr/bin/java
    which figtree                 # prints: /usr/bin/figtree
    which muttree                 # prints: [...]/muttree/src/muttree

__And now you can have fun!__


---


## Running muttree

The pipeline __requires__ the following input:

* __A coding sequence (CDS) alignment file, in FASTA format (`-i` option).__ Each sequence in the file should be composed of a concatenation of CDS sequences, where stop codons and trailing bases have been removed (i.e. the last codon of each CDS and — if the CDS length is not a multiple of 3 — any trailing bases after the last codon, have been removed before adding the CDS to the concatenated sequence). Each sequence in the FASTA alignment represents a sample, and must be labeled with the desired sample name. The first sequence in the file will be used as an outgroup sample to root the tree, so this should be the reference sequence or a suitable outgroup sample. An example can be found in the file [examples/Alignment_H3HASO.fna](examples/Alignment_H3HASO.fna).

* __A "gene table" (`-g` option).__ This is defined as a tab-delimited file with two columns (and no header): gene symbol and CDS start position (position of the first nucleotide in the concatenated sequence). This allows mapping each mutation to the gene where it occurs and finding recurrent mutations.

* __The full path to the output directory (`-o` option).__ The directory will be created if necessary. The pipeline implements a checkpoint logging system, so in the event that the execution is interrupted before finishing, re-running muttree with the same output directory will resume the make the pipeline resume the execution after the last successfully completed step.

Muttree also accepts some __optional__ input:

* __Custom options for RAxML (`-r` option).__ This allows personalizing the RAxML routine, which uses rapid bootstrapping followed by maximum likelihood search (see pipeline description below). Custom options must be specified as a single string within quotes, and must include all the required options for running RAxML, __except__ for the options `-s`, `-n`, `-w` and `-T`, which cannot be used.

* __Custom PAML control file (`-p` option).__ This allows personalizing the PAML `baseml` command settings, which are tuned by default for the analysis of coding sequence alignments (so modifications are not encouraged). The default options are in the file [treesub-TCG/resources/baseml.second.ctl](treesub-TCG/resources/baseml.second.ctl). The custom file cannot include the variables `seqfile`, `treefile` or `outfile`.

* __Number of RAxML threads (`-t` option).__ This allows using the multi-threaded version of RAxML to speed up the tree construction. This value can be any positive integer, and cannot be higher than the available number of processors. The default value is 1.

Following from this, the muttree command should be similar to this:

    muttree -i /path/to/alignment.fna -o /path/to/out_dir -g /path/to/gene_table.txt -t 8 -r "-m GTRGAMMA -# 10 -p 12345" -p /path/to/baseml.ctl

Full paths to files and directories should always be used. Most users should not need to use options `-r` and `-p`.

In addition, __running `muttree` without any arguments or with the `-h` option will print the help information__, whereas the `-v` option will only print the program version.


---


## Pipeline description

The pipeline is composed of the following steps:

1. __Adapting the input__
Treesub is used to adapt the input FASTA alignment to a format that is compatible with the tools employed by the pipeline.

2. __Maximum likelihood tree construction__
RAxML is used to build a maximum likelihood (ML) phylogenetic tree from the input alignment. This is by far the most expensive step of the pipeline. By default, rapid bootstrapping (with an extended majority­rule consensus tree stop criterion) is performed prior to the ML tree search, which employs a GTR substitution model plus a Gamma model of rate heterogeneity (`-f a -m GTRGAMMA -# autoMRE -x 931078 -p 272730` configuration; see the [RAxML manual](http://sco.h-its.org/exelixis/resource/download/NewManual.pdf)). However, custom RAxML options can be specified through the muttree `-r` option. Custom options must be specified between quotes (e.g. `-r "-m GTRGAMMA -# 10 -p 12345"`), and must include all the options required for running RAxML, __except__ for the options `-s`, `-n`, `-w` and `-T`, which cannot be used.

3. __Rooting the tree__
Treesub is used to root the ML tree by the outgroup sequence, which should be the first sequence in the input alignment FASTA file.

4. __Performing ancestral sequence reconstruction__
The PAML `baseml` command is used to estimate tree branch lengths and perform ancestral sequence reconstruction. The default baseml options, which are in the file [treesub-TCG/resources/baseml.second.ctl](treesub-TCG/resources/baseml.second.ctl), are tuned for the analysis of coding sequence alignments, so modifications are not encouraged (see the [PAML manual](http://abacus.gene.ucl.ac.uk/software/pamlDOC.pdf)). However, the path to a file with custom options can be specified using the muttree `-p` option. The custom file cannot include the variables `seqfile`, `treefile` or `outfile`.

5. __Annotating the tree__
Treesub is used to annotate the mutations occurring in each branch of the tree, based their reconstructed ancestral sequence. Mutations are assessed for their amino acid changes.

6. __Identifying recurrent mutations__
Finally, the input gene table is used to map each mutation to the gene (CDS) where it occurs, and any group of non-synonymous mutations affecting the same gene are marked as recurrent. A new tree is produced showing only the identified recurrent mutations in its branches.

Each one of the pipeline steps will generate an intermediate folder within the specified output directory, and a log file within the logs folder (also inside the output directory). The logs folder also contains the global pipeline log, as well as the checkpoint file, which is used to record the current stage of the pipeline and can be modified in order to restart the execution in any given step: when re-running muttree with the same output folder as before, execution which be resumed in the step that follows the last step recorded in the checkpoint file.

The pipeline's final output will be stored in a folder named Output, and will be consist of:

* A tab-delimited text file containing the information for all the identified mutations in the tree.

* Three versions of the same phylogenetic tree, in NEXUS format (compatible with FigTree):

    - One version showing bootstrap support values in its branch bifurcations.

    - One version showing all the identified mutations occurring in each branch.
    
    - One version showing the identified recurrent mutations occurring in each branch.



---


## License

Copyright © 2016 Transmissible Cancer Group, University of Cambridge  
Author: Adrian Baez-Ortega ([ORCID 0000-0002-9201-4420] (http://orcid.org/0000-0002-9201-4420); ab2324@cam.ac.uk)

Muttree is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses.


---


## References

1. Stamatakis, A. 2006. RAxML-VI-HPC: Maximum Likelihood-based Phylogenetic Analyses with Thousands of Taxa and Mixed Models. _Bioinformatics_ 22(21):2688–2690.

2. Yang, Z. 2007. PAML 4: Phylogenetic Analysis by Maximum Likelihood. _Molecular Biology and Evolution_ 24: 1586-1591.

3. Drummond, A., and K. Strimmer. 2001. PAL: An object-oriented programming library for molecular evolution and phylogenetics. _Bioinformatics_ 17: 662-663.

4. R.C.G. Holland; T. Down; M. Pocock; A. Prlić; D. Huen; K. James; S. Foisy; A. Dräger; A. Yates; M. Heuer; M.J. Schreiber. 2008. BioJava: an Open-Source Framework for Bioinformatics. _Bioinformatics_ 24 (18): 2096-2097.