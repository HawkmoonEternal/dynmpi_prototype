/*
 * Copyright (c) 2012-2020 Cisco Systems, Inc.  All rights reserved
 * Copyright (c) 2012      Los Alamos National Security, LLC. All rights reserved
 * Copyright (c) 2015-2020 Intel, Inc.  All rights reserved.
 * Copyright (c) 2021      Nanook Consulting.  All rights reserved.
 * $COPYRIGHT$
 *
 * Additional copyrights may follow
 *
 * $HEADER$
 */

/** @file
 *
 * This file provides a "hotel" class:
 *
 * - A hotel has a fixed number of rooms (i.e., storage slots)
 * - An arbitrary data pointer can check into an empty room at any time
 * - The occupant of a room can check out at any time
 * - Optionally, the occupant of a room can be forcibly evicted at a
 *   given time (i.e., when an prte timer event expires).
 * - The hotel has finite occupancy; if you try to checkin a new
 *   occupant and the hotel is already full, it will gracefully fail
 *   to checkin.
 *
 * One use case for this class is for ACK-based network retransmission
 * schemes (NACK-based retransmission schemes probably can use
 * prte_ring_buffer).
 *
 * For ACK-based retransmission schemes, a hotel might be used
 * something like this:
 *
 * - when a message is sent, check it in to a hotel with a timer
 * - if an ACK is received, check it out of the hotel (which also cancels
 *   the timer)
 * - if an ACK isn't received in time, the timer will expire and the
 *   upper layer will get a callback with the message
 * - if an ACK is received late (i.e., after its timer has expired),
 *   then checkout will gracefully fail
 *
 * Note that this class intentionally provides pretty minimal
 * functionality.  It is intended to be used in performance-critical
 * code paths -- extra functionality would simply add latency.
 *
 * There is an prte_hotel_init() function to create a hotel, but no
 * corresponding finalize; the destructor will handle all finalization
 * issues.  Note that when a hotel is destroyed, it will delete all
 * pending events from the event base (i.e., all pending eviction
 * callbacks); no further eviction callbacks will be invoked.
 */

#ifndef PRTE_HOTEL_H
#define PRTE_HOTEL_H

#include "prte_config.h"
#include "constants.h"
#include "prefetch.h"
#include "src/class/prte_object.h"
#include "src/event/event-internal.h"

BEGIN_C_DECLS

struct prte_hotel_t;

/* User-supplied function to be invoked when an occupant is evicted. */
typedef void (*prte_hotel_eviction_callback_fn_t)(struct prte_hotel_t *hotel, int room_num,
                                                  void *occupant);

/* Note that this is an internal data structure; it is not part of the
   public prte_hotel interface.  Public consumers of prte_hotel
   shouldn't need to use this struct at all (we only have it here in
   this .h file because some functions are inlined for speed, and need
   to get to the internals of this struct).

   The room struct should be as small as possible to be cache
   friendly.  Specifically: it would be great if multiple rooms could
   fit in a single cache line because we'll always allocate a
   contiguous set of rooms in an array. */
typedef struct {
    void *occupant;
    prte_event_t eviction_timer_event;
} prte_hotel_room_t;

/* Note that this is an internal data structure; it is not part of the
   public prte_hotel interface.  Public consumers of prte_hotel
   shouldn't need to use this struct at all (we only have it here in
   this .h file because some functions are inlined for speed, and need
   to get to the internals of this struct).

   Use a unique struct for holding the arguments for eviction
   callbacks.  We *could* make the to-be-evicted prte_hotel_room_t
   instance as the argument, but we don't, for 2 reasons:

   1. We want as many prte_hotel_room_t's to fit in a cache line as
      possible (i.e., to be as cache-friendly as possible).  The
      common/fast code path only needs to access the data in the
      prte_hotel_room_t (and not the callback argument data).

   2. Evictions will be uncommon, so we don't mind penalizing them a
      bit by making the data be in a separate cache line.
*/
typedef struct {
    struct prte_hotel_t *hotel;
    int room_num;
} prte_hotel_room_eviction_callback_arg_t;

typedef struct prte_hotel_t {
    /* make this an object */
    prte_object_t super;

    /* Max number of rooms in the hotel */
    int num_rooms;

    /* event base to be used for eviction timeout */
    prte_event_base_t *evbase;
    struct timeval eviction_timeout;
    prte_hotel_eviction_callback_fn_t evict_callback_fn;

    /* All rooms in this hotel */
    prte_hotel_room_t *rooms;

    /* Separate array for all the eviction callback arguments (see
       rationale above for why this is a separate array) */
    prte_hotel_room_eviction_callback_arg_t *eviction_args;

    /* where the next occupant will go */
    int last_unoccupied_room;
} prte_hotel_t;
PRTE_CLASS_DECLARATION(prte_hotel_t);

/**
 * Initialize the hotel.
 *
 * @param hotel Pointer to a hotel (IN)
 * @param num_rooms The total number of rooms in the hotel (IN)
 * @param evbase Pointer to event base used for eviction timeout
 * @param eviction_timeout Max length of a stay at the hotel before
 * the eviction callback is invoked (in seconds)
 * @param eviction_event_priority Event lib priority for the eviction timeout
 * @param evict_callback_fn Callback function invoked if an occupant
 * does not check out before the eviction_timeout.
 *
 * NOTE: If the callback function is NULL, then no eviction timer
 * will be set - occupants will remain checked into the hotel until
 * explicitly checked out.
 *
 * Also note: the eviction_callback_fn should absolutely not call any
 * of the hotel checkout functions.  Specifically: the occupant has
 * already been ("forcibly") checked out *before* the
 * eviction_callback_fn is invoked.
 *
 * @return PRTE_SUCCESS if all initializations were succesful. Otherwise,
 *  the error indicate what went wrong in the function.
 */
PRTE_EXPORT int prte_hotel_init(prte_hotel_t *hotel, int num_rooms, prte_event_base_t *evbase,
                                uint32_t eviction_timeout, int eviction_event_priority,
                                prte_hotel_eviction_callback_fn_t evict_callback_fn);

/**
 * Check in an occupant to the hotel.
 *
 * @param hotel Pointer to hotel (IN)
 * @param occupant Occupant to check in (opaque to the hotel) (IN)
 * @param room The room number that identifies this occupant in the
 * hotel (OUT).
 *
 * If there is room in the hotel, the occupant is checked in and the
 * timer for that occupant is started.  The occupant's room is
 * returned in the "room" param.
 *
 * Note that once a room's checkout_expire timer expires, the occupant
 * is forcibly checked out, and then the eviction callback is invoked.
 *
 * @return PRTE_SUCCESS if the occupant is successfully checked in,
 * and the room parameter will contain a valid value.
 * @return PRTE_ERR_TEMP_OUT_OF_RESOURCE is the hotel is full.  Try
 * again later.
 */
static inline int prte_hotel_checkin(prte_hotel_t *hotel, void *occupant, int *room_num)
{
    prte_hotel_room_t *room;
    int n;

    /* Do we have any rooms available? */
    if (PRTE_UNLIKELY(hotel->last_unoccupied_room < 0)) {
        return PRTE_ERR_OUT_OF_RESOURCE;
    }

    /* Put this occupant into the next empty room */
    *room_num = hotel->last_unoccupied_room;
    room = &(hotel->rooms[*room_num]);
    room->occupant = occupant;
    /* Assign the event and make it pending */
    if (NULL != hotel->evbase) {
        prte_event_add(&(room->eviction_timer_event), &(hotel->eviction_timeout));
    }

    /* find the next unoccupied room */
    n = hotel->last_unoccupied_room;
    room = &(hotel->rooms[n]);
    while (NULL != room->occupant && 0 != n) {
        --n;
        room = &(hotel->rooms[n]);
    }
    /* if we didn't find one, start over again at the top */
    if (NULL != room->occupant) {
        n = hotel->num_rooms - 1;
        room = &(hotel->rooms[n]);
        while (NULL != room->occupant && hotel->last_unoccupied_room != n) {
            --n;
            room = &(hotel->rooms[n]);
        }
    }
    /* if we couldn't find one, then mark this hotel as full */
    if (NULL != room->occupant) {
        hotel->last_unoccupied_room = -1;
    } else {
        hotel->last_unoccupied_room = n;
    }

    return PRTE_SUCCESS;
}

/**
 * Check the specified occupant out of the hotel.
 *
 * @param hotel Pointer to hotel (IN)
 * @param room Room number to checkout (IN)
 *
 * If there is an occupant in the room, their timer is canceled and
 * they are checked out.
 *
 * Nothing is returned (as a minor optimization).
 */
static inline void prte_hotel_checkout(prte_hotel_t *hotel, int room_num)
{
    prte_hotel_room_t *room;

    /* Bozo check */
    assert(room_num < hotel->num_rooms);

    /* If there's an occupant in the room, check them out */
    room = &(hotel->rooms[room_num]);
    if (PRTE_LIKELY(NULL != room->occupant)) {
        /* Do not change this logic without also changing the same
           logic in prte_hotel_checkout_and_return_occupant() and
           prte_hotel.c:local_eviction_callback(). */
        room->occupant = NULL;
        if (NULL != hotel->evbase) {
            prte_event_del(&(room->eviction_timer_event));
        }
    }

    /* Don't bother returning whether we actually checked someone out
       or not (because this is in the critical performance path) --
       assume the upper layer knows what it's doing. */
}

/**
 * Check the specified occupant out of the hotel and return the occupant.
 *
 * @param hotel Pointer to hotel (IN)
 * @param room Room number to checkout (IN)
 * @param void * occupant (OUT)
 * If there is an occupant in the room, their timer is canceled and
 * they are checked out.
 *
 * Use this checkout and when caller needs the occupant
 */
static inline void prte_hotel_checkout_and_return_occupant(prte_hotel_t *hotel, int room_num,
                                                           void **occupant)
{
    prte_hotel_room_t *room;

    /* Bozo check */
    assert(room_num < hotel->num_rooms);

    /* If there's an occupant in the room, check them out */
    room = &(hotel->rooms[room_num]);
    if (PRTE_LIKELY(NULL != room->occupant)) {
        /* Do not change this logic without also changing the same
           logic in prte_hotel_checkout() and
           prte_hotel.c:local_eviction_callback(). */
        *occupant = room->occupant;
        room->occupant = NULL;
        if (NULL != hotel->evbase) {
            prte_event_del(&(room->eviction_timer_event));
        }
    } else {
        *occupant = NULL;
    }
}

/**
 * Returns true if the hotel is empty (no occupant)
 * @param hotel Pointer to hotel (IN)
 * @return bool true if empty false if there is a occupant(s)
 *
 */
static inline bool prte_hotel_is_empty(prte_hotel_t *hotel)
{
    prte_hotel_room_t *room;
    int n;

    for (n=0; n < hotel->num_rooms; n++) {
        room = &(hotel->rooms[n]);
        if (NULL != room->occupant) {
            return false;
        }
    }
    return true;
}

/**
 * Access the occupant of a room, but leave them checked into their room.
 *
 * @param hotel Pointer to hotel (IN)
 * @param room Room number to checkout (IN)
 * @param void * occupant (OUT)
 *
 * This accessor function is typically used to cycle across the occupants
 * to check for someone already present that matches a description.
 */
static inline void prte_hotel_knock(prte_hotel_t *hotel, int room_num, void **occupant)
{
    prte_hotel_room_t *room;

    /* Bozo check */
    assert(room_num < hotel->num_rooms);

    *occupant = NULL;

    /* If there's an occupant in the room, have them come to the door */
    room = &(hotel->rooms[room_num]);
    if (PRTE_LIKELY(NULL != room->occupant)) {
        prte_output(10, "occupant %p in room num %d responded to knock", room->occupant, room_num);
        *occupant = room->occupant;
    }
}

END_C_DECLS

#endif /* PRTE_HOTEL_H */
