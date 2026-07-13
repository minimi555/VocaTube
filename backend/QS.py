from typing import List

QS_150_list : List[str] = [
    "mit.edu",                          # 1 Massachusetts Institute of Technology (MIT)
    "imperial.ac.uk",                   # 2 Imperial College London
    "stanford.edu",                     # 2 Stanford University
    "ox.ac.uk",                         # 4 University of Oxford
    "harvard.edu",                      # 5 Harvard University
    "cam.ac.uk",                        # 6 University of Cambridge
    "caltech.edu",                      # 7 California Institute of Technology (Caltech)
    "ethz.ch",                          # 8 ETH Zurich
    "ucl.ac.uk",                        # 8 UCL
    "nus.edu.sg",                       # 10 National University of Singapore (NUS)
    "hku.hk",                           # 11 The University of Hong Kong
    "ntu.edu.sg",                       # 12 Nanyang Technological University, Singapore (NTU Singapore)
    "pku.edu.cn",                       # 13 Peking University
    "tsinghua.edu.cn",                  # 14 Tsinghua University
    "upenn.edu",                        # 15 University of Pennsylvania
    "cornell.edu",                      # 16 Cornell University
    "yale.edu",                         # 16 Yale University
    "cuhk.edu.hk",                      # 18 The Chinese University of Hong Kong (CUHK)
    "unsw.edu.au",                      # 19 The University of New South Wales (UNSW Sydney)
    "jhu.edu",                          # 20 Johns Hopkins University
    "berkeley.edu",                     # 20 University of California, Berkeley (UCB)
    "epfl.ch",                          # 22 EPFL - Ecole polytechnique federale de Lausanne
    "unimelb.edu.au",                   # 22 The University of Melbourne
    "uchicago.edu",                     # 24 University of Chicago
    "tum.de",                           # 25 Technical University of Munich
    "fudan.edu.cn",                     # 26 Fudan University
    "princeton.edu",                    # 27 Princeton University
    "sydney.edu.au",                    # 28 The University of Sydney
    "anu.edu.au",                       # 29 Australian National University (ANU)
    "mcgill.ca",                        # 30 McGill University
    "monash.edu",                       # 31 Monash University
    "utoronto.ca",                      # 32 University of Toronto
    "hkust.edu.hk",                     # 33 The Hong Kong University of Science and Technology
    "psl.eu",                           # 34 Universite PSL
    "ed.ac.uk",                         # 35 The University of Edinburgh
    "sjtu.edu.cn",                      # 36 Shanghai Jiao Tong University
    "kcl.ac.uk",                        # 37 King's College London
    "snu.ac.kr",                        # 38 Seoul National University
    "u-tokyo.ac.jp",                    # 39 The University of Tokyo
    "manchester.ac.uk",                 # 40 The University of Manchester
    "uq.edu.au",                        # 40 The University of Queensland
    "yonsei.ac.kr",                     # 42 Yonsei University
    "columbia.edu",                     # 43 Columbia University
    "ip-paris.fr",                      # 43 Institut Polytechnique de Paris
    "northwestern.edu",                 # 45 Northwestern University
    "ubc.ca",                           # 45 University of British Columbia
    "zju.edu.cn",                       # 47 Zhejiang University
    "tudelft.nl",                       # 48 Delft University of Technology
    "ucla.edu",                         # 49 University of California, Los Angeles (UCLA)
    "polyu.edu.hk",                     # 50 The Hong Kong Polytechnic University
    "umich.edu",                        # 51 University of Michigan-Ann Arbor
    "cityu.edu.hk",                     # 52 City University of Hong Kong (CityUHK)
    "korea.ac.kr",                      # 52 Korea University
    "ntu.edu.tw",                       # 54 National Taiwan University (NTU)
    "cmu.edu",                          # 55 Carnegie Mellon University
    "um.edu.my",                        # 56 Universiti Malaya (UM)
    "bristol.ac.uk",                    # 57 University of Bristol
    "nyu.edu",                          # 58 New York University (NYU)
    "kuleuven.be",                      # 59 KU Leuven
    "uva.nl",                           # 60 University of Amsterdam
    "lmu.de",                           # 61 Ludwig-Maximilians-Universitaet Muenchen
    "lse.ac.uk",                        # 62 The London School of Economics and Political Science (LSE)
    "kfupm.edu.sa",                     # 63 KFUPM
    "kyoto-u.ac.jp",                    # 64 Kyoto University
    "kaist.ac.kr",                      # 65 KAIST
    "brown.edu",                        # 66 Brown University
    "auckland.ac.nz",                   # 67 The University of Auckland
    "warwick.ac.uk",                    # 68 The University of Warwick
    "birmingham.ac.uk",                 # 68 University of Birmingham
    "duke.edu",                         # 70 Duke University
    "lu.se",                            # 71 Lund University
    "utexas.edu",                       # 72 University of Texas at Austin
    "sorbonne-universite.fr",           # 73 Sorbonne University
    "illinois.edu",                     # 74 University of Illinois Urbana-Champaign
    "tcd.ie",                           # 75 Trinity College Dublin, The University of Dublin
    "universite-paris-saclay.fr",       # 76 Universite Paris-Saclay
    "uwa.edu.au",                       # 77 The University of Western Australia
    "leeds.ac.uk",                      # 77 University of Leeds
    "adelaide.edu.au",                  # 79 Adelaide University
    "gla.ac.uk",                        # 80 University of Glasgow
    "ucsd.edu",                         # 81 University of California, San Diego (UCSD)
    "kth.se",                           # 82 KTH Royal Institute of Technology
    "sheffield.ac.uk",                  # 82 The University of Sheffield
    "uba.ar",                           # 84 Universidad de Buenos Aires (UBA)
    "durham.ac.uk",                     # 85 Durham University
    "uni-heidelberg.de",                # 86 Universitaet Heidelberg
    "polimi.it",                        # 87 Politecnico di Milano
    "uts.edu.au",                       # 87 University of Technology Sydney
    "uu.se",                            # 87 Uppsala University
    "nju.edu.cn",                       # 90 Nanjing University
    "ku.dk",                            # 90 University of Copenhagen
    "psu.edu",                          # 92 Pennsylvania State University
    "washington.edu",                   # 92 University of Washington
    "bu.edu",                           # 94 Boston University
    "osaka-u.ac.jp",                    # 95 The University of Osaka
    "ualberta.ca",                      # 96 University of Alberta
    "isct.ac.jp",                       # 97 Institute of Science Tokyo
    "nottingham.ac.uk",                 # 97 University of Nottingham
    "fu-berlin.de",                     # 98 Freie Universitaet Berlin
    "uzh.ch",                           # 98 University of Zurich
    "purdue.edu",                       # 100 Purdue University
    "ucd.ie",                           # 100 University College Dublin
    "tohoku.ac.jp",                     # 102 Tohoku University
    "qmul.ac.uk",                       # 103 Queen Mary University of London
    "rwth-aachen.de",                   # 104 RWTH Aachen University
    "dtu.dk",                           # 105 Technical University of Denmark
    "postech.ac.kr",                    # 106 Pohang University of Science And Technology (POSTECH)
    "ksu.edu.sa",                       # 107 King Saud University
    "skku.edu",                         # 108 Sungkyunkwan University (SKKU)
    "qu.edu.qa",                        # 109 Qatar University
    "kit.edu",                          # 110 KIT, Karlsruhe Institute of Technology
    "uniroma1.it",                      # 111 Sapienza University of Rome
    "southampton.ac.uk",                # 111 University of Southampton
    "uwaterloo.ca",                     # 113 University of Waterloo
    "uu.nl",                            # 113 Utrecht University
    "msu.ru",                           # 115 Lomonosov Moscow State University
    "st-andrews.ac.uk",                 # 115 University of St Andrews
    "iitd.ac.in",                       # 118 Indian Institute of Technology Delhi (IITD)
    "leidenuniv.nl",                    # 119 Leiden University
    "uc.cl",                            # 119 Pontificia Universidad Catolica de Chile (UC)
    "rmit.edu.au",                      # 119 RMIT University
    "rice.edu",                         # 122 Rice University
    "unibo.it",                         # 123 Alma Mater Studiorum - Universita di Bologna
    "helsinki.fi",                      # 123 University of Helsinki
    "bath.ac.uk",                       # 125 University of Bath
    "aalto.fi",                         # 126 Aalto University
    "mq.edu.au",                        # 126 Macquarie University (Sydney, Australia)
    "au.dk",                            # 128 Aarhus University
    "usm.my",                           # 128 Universiti Sains Malaysia (USM)
    "ukm.edu.my",                       # 130 Universiti Kebangsaan Malaysia (UKM)
    "uio.no",                           # 131 University of Oslo
    "wisc.edu",                         # 131 University of Wisconsin-Madison
    "usp.br",                           # 133 Universidade de Sao Paulo
    "iitb.ac.in",                       # 134 Indian Institute of Technology Bombay (IITB)
    "ustc.edu.cn",                      # 134 University of Science and Technology of China
    "exeter.ac.uk",                     # 136 University of Exeter
    "ucdavis.edu",                      # 137 University of California, Davis
    "upm.edu.my",                       # 138 Universiti Putra Malaysia (UPM)
    "liverpool.ac.uk",                  # 139 University of Liverpool
    "hu-berlin.de",                     # 140 Humboldt-Universitaet zu Berlin
    "univie.ac.at",                     # 140 University of Vienna
    "gatech.edu",                       # 142 Georgia Institute of Technology
    "nthu.edu.tw",                      # 142 National Tsing Hua University - NTHU
    "uwo.ca",                           # 142 Western University
    "unam.mx",                          # 145 Universidad Nacional Autonoma de Mexico (UNAM)
    "tongji.edu.cn",                    # 146 Tongji University
    "ku.ac.ae",                         # 147 Khalifa University
    "eur.nl",                           # 148 Erasmus University Rotterdam
    "ncl.ac.uk",                        # 149 Newcastle University
    "ugent.be",                         # 150 Ghent University
    "unibas.ch",                        # 150 University of Basel
]