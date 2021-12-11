/*
 * Copyright (c) 2004-2007 The Trustees of Indiana University and Indiana
 *                         University Research and Technology
 *                         Corporation.  All rights reserved.
 * Copyright (c) 2004-2006 The University of Tennessee and The University
 *                         of Tennessee Research Foundation.  All rights
 *                         reserved.
 * Copyright (c) 2004-2005 High Performance Computing Center Stuttgart,
 *                         University of Stuttgart.  All rights reserved.
 * Copyright (c) 2004-2005 The Regents of the University of California.
 *                         All rights reserved.
 * Copyright (c) 2006-2013 Los Alamos National Security, LLC.
 *                         All rights reserved.
 * Copyright (c) 2010-2020 Cisco Systems, Inc.  All rights reserved
 * Copyright (c) 2019      Intel, Inc.  All rights reserved.
 * Copyright (c) 2019      Research Organization for Information Science
 *                         and Technology (RIST).  All rights reserved.
 * Copyright (c) 2021      Nanook Consulting.  All rights reserved.
 * $COPYRIGHT$
 *
 * Additional copyrights may follow
 *
 * $HEADER$
 */

#ifndef _MCA_OOB_TCP_LISTENER_H_
#define _MCA_OOB_TCP_LISTENER_H_

#include "prte_config.h"

#ifdef HAVE_SYS_TYPES_H
#    include <sys/types.h>
#endif
#ifdef HAVE_SYS_SOCKET_H
#    include <sys/socket.h>
#endif

#include "src/class/prte_list.h"
#include "src/event/event-internal.h"

/*
 * Data structure for accepting connections.
 */
struct prte_oob_tcp_listener_t {
    prte_list_item_t item;
    bool ev_active;
    prte_event_t event;
    bool tcp6;
    int sd;
    uint16_t port;
};
typedef struct prte_oob_tcp_listener_t prte_oob_tcp_listener_t;
PRTE_CLASS_DECLARATION(prte_oob_tcp_listener_t);

typedef struct {
    prte_object_t super;
    prte_event_t ev;
    int fd;
    struct sockaddr_storage addr;
} prte_oob_tcp_pending_connection_t;
PRTE_CLASS_DECLARATION(prte_oob_tcp_pending_connection_t);

PRTE_MODULE_EXPORT int prte_oob_tcp_start_listening(void);

#endif /* _MCA_OOB_TCP_LISTENER_H_ */
