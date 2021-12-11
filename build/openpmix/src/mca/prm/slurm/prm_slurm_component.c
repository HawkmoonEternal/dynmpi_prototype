/* -*- Mode: C; c-basic-offset:4 ; indent-tabs-mode:nil -*- */
/*
 * Copyright (c) 2016-2019 Intel, Inc.  All rights reserved.
 * Copyright (c) 2019      Research Organization for Information Science
 *                         and Technology (RIST).  All rights reserved.
 * Copyright (c) 2020      Cisco Systems, Inc.  All rights reserved
 * Copyright (c) 2021      Nanook Consulting.  All rights reserved.
 * $COPYRIGHT$
 *
 * Additional copyrights may follow
 *
 * $HEADER$
 */

#include "pmix_config.h"

#include "src/util/show_help.h"

#include "src/mca/prm/prm.h"
#include "prm_slurm.h"

static int component_query(pmix_mca_base_module_t **module, int *priority);

/*
 * Struct of function pointers and all that to let us be initialized
 */
pmix_prm_base_component_t mca_prm_slurm_component = {
    .base = {
        PMIX_PRM_BASE_VERSION_1_0_0,
        .pmix_mca_component_name = "slurm",
        PMIX_MCA_BASE_MAKE_VERSION(component, PMIX_MAJOR_VERSION, PMIX_MINOR_VERSION,
                                    PMIX_RELEASE_VERSION),
        .pmix_mca_query_component = component_query,
    },
    .data = {
        /* The component is checkpoint ready */
        PMIX_MCA_BASE_METADATA_PARAM_CHECKPOINT
    },
};

static int component_query(pmix_mca_base_module_t **module, int *priority)
{
    /* disqualify ourselves if we are not under slurm */
    if (NULL == getenv("SLURM_JOBID")) {
        *priority = 0;
        *module = NULL;
        return PMIX_ERROR;
    }

    *module = (pmix_mca_base_module_t*)&pmix_prm_slurm_module;
    *priority = 50;
    return PMIX_SUCCESS;
}
