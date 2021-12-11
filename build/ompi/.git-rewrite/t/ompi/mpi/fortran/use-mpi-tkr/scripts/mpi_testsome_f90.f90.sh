#! /bin/sh

#
# Copyright (c) 2004-2006 The Trustees of Indiana University and Indiana
#                         University Research and Technology
#                         Corporation.  All rights reserved.
# Copyright (c) 2004-2005 The Regents of the University of California.
#                         All rights reserved.
# Copyright (c) 2006-2012 Cisco Systems, Inc.  All rights reserved.
# $COPYRIGHT$
# 
# Additional copyrights may follow
# 
# $HEADER$
#

# This flie was not generated by XML scripts; it was written by hand.
# It may be generated someday, but at this point, it was simpler to
# just write it by hand.

. "$1/fortran_kinds.sh"

# This entire file is only generated in small or larger modules.  So
# if we're not at least small, bail now.

check_size small
if test "$output" = "0"; then
    exit 0
fi

# Ok, we should continue.

output() {
    suffix=$1
    status_type=$2

    cat <<EOF

subroutine MPI_Testsome${suffix}(incount, array_of_requests, outcount, array_of_indices, array_of_statuses&
        , ierror)
  include 'mpif-config.h'
  integer, intent(in) :: incount
  integer, dimension(incount), intent(inout) :: array_of_requests
  integer, intent(out) :: outcount
  integer, dimension(*), intent(out) :: array_of_indices
  $status_type, intent(out) :: array_of_statuses
  integer, intent(out) :: ierror

  call MPI_Testsome(incount, array_of_requests, outcount, array_of_indices, array_of_statuses, ierror)
end subroutine MPI_Testsome${suffix}

EOF
}

# S = array of statuses, I = STATUSES_IGNORE

output S "integer, dimension(MPI_STATUS_SIZE, incount)"
output I "double precision"
