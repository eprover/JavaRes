#!/bin/bash
# Usage: testall /home/apease/Programs/TPTP-v5.2.0/Problems/PUZ/*.p

java -XX:-UseGCOverheadLimit -Xmx2000m -classpath /home/apease/EProver/fod_pi/Java/build/classes atp.Prover2 -to 2 $1

