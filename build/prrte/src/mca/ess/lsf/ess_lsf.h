/*
 * Copyright (c) 2004-2008 The Trustees of Indiana University and Indiana
 *                         University Research and Technology
 *                         Corporation.  All rights reserved.
 * Copyright (c) 2004-2005 The University of Tennessee and The University
 *                         of Tennessee Research Foundation.  All rights
 *                         reserved.
 * Copyright (c) 2004-2005 High Performance Computing Center Stuttgart,
 *                         University of Stuttgart.  All rights reserved.
 * Copyright (c) 2004-2005 The Regents of the University of California.
 *                         All rights reserved.
 * Copyright (c) 2007-2020 Cisco Systems, Inc.  All rights reserved
 * Copyright (c) 2019      Intel, Inc.  All rights reserved.
 * Copyright (c) 2019      Research Organization for Information Science
 *                         and Technology (RIST).  All rights reserved.
 * $COPYRIGHT$
 *
 * Additional copyrights may follow
 *
 * $HEADER$
 */

#ifndef PRTE_ESS_LSF_H
#define PRTE_ESS_LSF_H

BEGIN_C_DECLS

PRTE_MODULE_EXPORT extern prte_ess_base_component_t prte_ess_lsf_component;

/*
 * Module open / close
 */
int prte_ess_lsf_component_open(void);
int prte_ess_lsf_component_close(void);
int prte_ess_lsf_component_query(prte_mca_base_module_t **module, int *priority);

END_C_DECLS

#endif /* PRTE_ESS_LSF_H */
