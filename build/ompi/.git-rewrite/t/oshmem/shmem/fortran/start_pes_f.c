/*
 * Copyright (c) 2013      Mellanox Technologies, Inc.
 *                         All rights reserved.
 * Copyright (c) 2013 Cisco Systems, Inc.  All rights reserved.
 * $COPYRIGHT$
 * 
 * Additional copyrights may follow
 * 
 * $HEADER$
 */

#include "oshmem_config.h"
#include "oshmem/shmem/fortran/bindings.h"
#include "oshmem/include/shmem.h"

SHMEM_GENERATE_FORTRAN_BINDINGS_SUB (void,
        START_PES,
        start_pes_,
        start_pes__,
        start_pes_f,
        (MPI_Fint npes), 
        (npes) )

void start_pes_f(MPI_Fint npes)
{
    shmem_init();
}

