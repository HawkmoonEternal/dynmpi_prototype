/* -*- Mode: C; c-basic-offset:4 ; indent-tabs-mode:nil -*- */
/*
 * Copyright (c) 2004-2005 The Trustees of Indiana University and Indiana
 *                         University Research and Technology
 *                         Corporation.  All rights reserved.
 * Copyright (c) 2004-2005 The University of Tennessee and The University
 *                         of Tennessee Research Foundation.  All rights
 *                         reserved.
 * Copyright (c) 2004-2005 High Performance Computing Center Stuttgart,
 *                         University of Stuttgart.  All rights reserved.
 * Copyright (c) 2004-2005 The Regents of the University of California.
 *                         All rights reserved.
 * Copyright (c) 2011-2013 Cisco Systems, Inc.  All rights reserved.
 * Copyright (c) 2013      Los Alamos National Security, LLC. All rights
 *                         reserved.
 * Copyright (c) 2015      Research Organization for Information Science
 *                         and Technology (RIST). All rights reserved.
 * Copyright (c) 2019      Triad National Security, LLC.  All rights reserved.
 *
 * $COPYRIGHT$
 *
 * Additional copyrights may follow
 *
 * $HEADER$
 */

#include "ompi_config.h"

#include "ompi/mpi/fortran/mpif-h/bindings.h"

#if OMPI_BUILD_MPI_PROFILING
#if OPAL_HAVE_WEAK_SYMBOLS
#pragma weak PMPI_SESSION_INIT = ompi_session_init_f
#pragma weak pmpi_session_init = ompi_session_init_f
#pragma weak pmpi_session_init_ = ompi_session_init_f
#pragma weak pmpi_session_init__ = ompi_session_init_f

#pragma weak PMPI_Session_init_f = ompi_session_init_f
#pragma weak PMPI_Session_init_f08 = ompi_session_init_f
#else
OMPI_GENERATE_F77_BINDINGS (PMPI_SESSION_INIT,
                            pmpi_session_init,
                            pmpi_session_init_,
                            pmpi_session_init__,
                            pompi_session_init_f,
                            (MPI_Fint *info, MPI_Fint *errhandler, MPI_Fint *session, MPI_Fint *ierr),
                            (info, errhandler, session, ierr) )
#endif
#endif

#if OPAL_HAVE_WEAK_SYMBOLS
#pragma weak MPI_SESSION_INIT = ompi_session_init_f
#pragma weak mpi_session_init = ompi_session_init_f
#pragma weak mpi_session_init_ = ompi_session_init_f
#pragma weak mpi_session_init__ = ompi_session_init_f

#pragma weak MPI_Session_init_f = ompi_session_init_f
#pragma weak MPI_Session_init_f08 = ompi_session_init_f
#else
#if ! OMPI_BUILD_MPI_PROFILING
OMPI_GENERATE_F77_BINDINGS (MPI_SESSION_INIT,
                            mpi_session_init,
                            mpi_session_init_,
                            mpi_session_init__,
                            ompi_session_init_f,
                            (MPI_Fint *info, MPI_Fint *errhandler, MPI_Fint *session, MPI_Fint *ierr),
                            (info, errhandler, session, ierr) )
#else
#define ompi_session_init_f pompi_session_init_f
#endif
#endif

void ompi_session_init_f(MPI_Fint *info, MPI_Fint *errhandler, MPI_Fint *session, MPI_Fint *ierr)
{
    int c_ierr;
    MPI_Session c_session;
    MPI_Info c_info;
    MPI_Errhandler c_errhandler;

    c_info = PMPI_Info_f2c(*info);
    c_errhandler = PMPI_Errhandler_f2c(*errhandler);

    c_ierr = PMPI_Session_init(c_info, c_errhandler, &c_session);
    if (NULL != ierr) *ierr = OMPI_INT_2_FINT(c_ierr);

    if (MPI_SUCCESS == c_ierr) {
        *session = PMPI_Session_c2f(c_session);
    }
}
