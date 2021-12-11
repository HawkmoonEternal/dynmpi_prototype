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
 * Copyright (c) 2019      Intel, Inc.  All rights reserved.
 * Copyright (c) 2020      Cisco Systems, Inc.  All rights reserved
 * Copyright (c) 2021      Nanook Consulting.  All rights reserved.
 * $COPYRIGHT$
 *
 * Additional copyrights may follow
 *
 * $HEADER$
 */

#include "prte_config.h"
#include "constants.h"

#include <string.h>

#include "src/mca/base/base.h"
#include "src/mca/mca.h"

#include "src/mca/rmaps/base/base.h"

static bool selected = false;

/*
 * Function for selecting one component from all those that are
 * available.
 */
int prte_rmaps_base_select(void)
{
    prte_mca_base_component_list_item_t *cli = NULL;
    prte_mca_base_component_t *component = NULL;
    prte_mca_base_module_t *module = NULL;
    prte_rmaps_base_module_t *nmodule;
    prte_rmaps_base_selected_module_t *newmodule, *mod;
    int rc, priority;
    bool inserted;

    if (selected) {
        /* ensure we don't do this twice */
        return PRTE_SUCCESS;
    }
    selected = true;

    /* Query all available components and ask if they have a module */
    PRTE_LIST_FOREACH(cli, &prte_rmaps_base_framework.framework_components,
                      prte_mca_base_component_list_item_t)
    {
        component = (prte_mca_base_component_t *) cli->cli_component;

        prte_output_verbose(5, prte_rmaps_base_framework.framework_output,
                            "mca:rmaps:select: checking available component %s",
                            component->mca_component_name);

        /* If there's no query function, skip it */
        if (NULL == component->mca_query_component) {
            prte_output_verbose(
                5, prte_rmaps_base_framework.framework_output,
                "mca:rmaps:select: Skipping component [%s]. It does not implement a query function",
                component->mca_component_name);
            continue;
        }

        /* Query the component */
        prte_output_verbose(5, prte_rmaps_base_framework.framework_output,
                            "mca:rmaps:select: Querying component [%s]",
                            component->mca_component_name);
        rc = component->mca_query_component(&module, &priority);

        /* If no module was returned, then skip component */
        if (PRTE_SUCCESS != rc || NULL == module) {
            prte_output_verbose(
                5, prte_rmaps_base_framework.framework_output,
                "mca:rmaps:select: Skipping component [%s]. Query failed to return a module",
                component->mca_component_name);
            continue;
        }

        /* If we got a module, keep it */
        nmodule = (prte_rmaps_base_module_t *) module;
        /* add to the list of selected modules */
        newmodule = PRTE_NEW(prte_rmaps_base_selected_module_t);
        newmodule->pri = priority;
        newmodule->module = nmodule;
        newmodule->component = component;

        /* maintain priority order */
        inserted = false;
        PRTE_LIST_FOREACH(mod, &prte_rmaps_base.selected_modules, prte_rmaps_base_selected_module_t)
        {
            if (priority > mod->pri) {
                prte_list_insert_pos(&prte_rmaps_base.selected_modules, (prte_list_item_t *) mod,
                                     &newmodule->super);
                inserted = true;
                break;
            }
        }
        if (!inserted) {
            /* must be lowest priority - add to end */
            prte_list_append(&prte_rmaps_base.selected_modules, &newmodule->super);
        }
    }

    if (4 < prte_output_get_verbosity(prte_rmaps_base_framework.framework_output)) {
        prte_output(0, "%s: Final mapper priorities", PRTE_NAME_PRINT(PRTE_PROC_MY_NAME));
        /* show the prioritized list */
        PRTE_LIST_FOREACH(mod, &prte_rmaps_base.selected_modules, prte_rmaps_base_selected_module_t)
        {
            prte_output(0, "\tMapper: %s Priority: %d", mod->component->mca_component_name,
                        mod->pri);
        }
    }

    return PRTE_SUCCESS;
    ;
}