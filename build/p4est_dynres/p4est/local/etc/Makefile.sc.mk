
# This file is part of the SC Library
# Use `include /path/to/Makefile.sc.mk' in your Makefile
# to use libsc in your project without autotools

sc_prefix = /dss/dssfs02/lwp-dss-0001/pr63qi/pr63qi-dss-0000/ga84kaf2/shared-folder/build/p4est_dynres/p4est/local
sc_exec_prefix = ${sc_prefix}
sc_sysconfdir = ${sc_prefix}/etc

# SC_CC and SC_CFLAGS may not be necessary for your project
SC_CC = /dss/dssfs02/lwp-dss-0001/pr63qi/pr63qi-dss-0000/ga84kaf2/shared-folder/install/ompi/bin/mpicc
SC_CFLAGS = -g -O2

# These pull in sc but none of its dependencies
SC_PKG_CPPFLAGS = -I${sc_prefix}/include
SC_PKG_LDFLAGS = -L${sc_exec_prefix}/lib
SC_PKG_LIBS = -lsc

# These pull in everything needed by libsc
SC_CPPFLAGS =  $(SC_PKG_CPPFLAGS)
SC_LDFLAGS =  $(SC_PKG_LDFLAGS)
SC_LIBS = $(SC_PKG_LIBS)   -lz -lm   
