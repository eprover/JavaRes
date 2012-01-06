#!/bin/bash
# Usage: ./testall2.sh /home/apease/Programs/TPTP-v5.3.0/Problems/PUZ/*.p

java -XX:-UseGCOverheadLimit -Xmx2000m -classpath /home/apease/EProver/fod_pi/Java/build/classes atp.Prover2 \
	-i /home/apease/Programs/TPTP-v5.3.0 -to 20 -f --csvstats $1

