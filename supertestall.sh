#!/bin/bash
# Usage: 

find /home/apease/Programs/TPTP-v5.3.0/Problems/CSR -name '*.p' \( -exec ./testall2.sh {} \; -o -print \)


