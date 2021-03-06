/*
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
/*
 * File         : Comm.java
 * Author       : Sang Lim, Sung-Hoon Ko, Xinying Li, Bryan Carpenter
 * Created      : Thu Apr  9 12:22:15 1998
 * Revision     : $Revision: 1.20 $
 * Updated      : $Date: 2001/08/07 16:36:25 $
 * Copyright: Northeast Parallel Architectures Center
 *            at Syracuse University 1998
 */
package mpi;

import java.nio.*;
import static mpi.MPI.isHeapBuffer;
import static mpi.MPI.assertDirectBuffer;

/**
 * The {@code Comm} class represents communicators.
 */
public class Comm implements Freeable
{
protected final static int SELF  = 1;
protected final static int WORLD = 2;
protected long handle;

protected static long nullHandle;

static
{
    init();
}

private static native void init();

protected Comm()
{
}

protected Comm(long handle)
{
    this.handle = handle;
}

protected final void setType(int type)
{
    getComm(type);
}

private native void getComm(int type);

/**
 * Duplicate this communicator.
 * <p>Java binding of the MPI operation {@code MPI_COMM_DUP}.
 * <p>The new communicator is "congruent" to the old one,
 *    but has a different context.
 * @return copy of this communicator
 */
@Override public Comm clone()
{
    try
    {
        MPI.check();
        return new Comm(dup());
    }
    catch(MPIException e)
    {
        throw new RuntimeException(e.getMessage());
    }
}

protected final native long dup() throws MPIException;

/**
 * Size of group of this communicator.
 * <p>Java binding of the MPI operation {@code MPI_COMM_SIZE}.
 * @return number of processors in the group of this communicator
 * @throws MPIException
 */
public final int getSize() throws MPIException
{
    MPI.check();
    return getSize(handle);
}

private native int getSize(long comm) throws MPIException;

/**
 * Rank of this process in group of this communicator.
 * <p>Java binding of the MPI operation {@code MPI_COMM_RANK}.
 * @return rank of the calling process in the group of this communicator
 * @throws MPIException
 */
public final int getRank() throws MPIException
{
    MPI.check();
    return getRank(handle);
}

private native int getRank(long comm) throws MPIException;

/**
 * Compare two communicators.
 * <p>Java binding of the MPI operation {@code MPI_COMM_COMPARE}.
 * @param comm1 first communicator
 * @param comm2 second communicator
 * @return
 * {@code MPI.IDENT} results if the {@code comm1} and {@code comm2}
 * are references to the same object (ie, if {@code comm1 == comm2}).<br>
 * {@code MPI.CONGRUENT} results if the underlying groups are identical
 * but the communicators differ by context.<br>
 * {@code MPI.SIMILAR} results if the underlying groups are similar
 * but the communicators differ by context.<br>
 * {@code MPI.UNEQUAL} results otherwise.
 * @throws MPIException
 */
public static int compare(Comm comm1, Comm comm2) throws MPIException
{
    MPI.check();
    return compare(comm1.handle, comm2.handle);
}

private static native int compare(long comm1, long comm2) throws MPIException;

/**
 * Java binding of the MPI operation {@code MPI_COMM_FREE}.
 * @throws MPIException
 */
@Override public void free() throws MPIException
{
    MPI.check();
    handle = free(handle);
}

private native long free(long comm) throws MPIException;

/**
 * Java binding of the MPI operation {@code MPI_COMM_DISCONNECT}.
 * @throws MPIException 
 */
public final void disconnect() throws MPIException
{
    MPI.check();
    handle = disconnect(handle);
}

private native long disconnect(long comm) throws MPIException;

/**
 * Test if communicator object is void (has been freed).
 * @return true if the comm object is void, false otherwise
 */
public final native boolean isNull();

/**
 * Return group associated with a communicator.
 * <p>Java binding of the MPI operation {@code MPI_COMM_GROUP}.
 * @return group corresponding to this communicator group
 * @throws MPIException
 */
public final Group getGroup() throws MPIException
{
    MPI.check();
    return new Group(getGroup(handle));
}

private native long getGroup(long comm);

// Inter-communication

/**
 * Test if this communicator is an inter-communicator.
 * <p>Java binding of the MPI operation {@code MPI_COMM_TEST_INTER}.
 * @return {@code true} if this is an inter-communicator,
 *         {@code false} otherwise
 * @throws MPIException
 */
public final boolean isInter() throws MPIException
{
    MPI.check();
    return isInter(handle);
}

private native boolean isInter(long comm) throws MPIException;

/**
 * Create an inter-communicator.
 * <p>
 * Java binding of the MPI operation {@code MPI_INTERCOMM_CREATE}.
 * <p>
 * This operation is defined as a method on the "peer communicator",
 * making it analogous to a {@code send} or {@code recv} communication
 * with the remote group leader.
 * @param localComm    local intra-communicator
 * @param localLeader  rank of local group leader in {@code localComm}
 * @param remoteLeader rank of remote group leader in this communicator
 * @param tag          "safe" tag
 * @return new inter-communicator
 * @throws MPIException
 */
public final Intercomm createIntercomm(Comm localComm, int localLeader,
                                       int remoteLeader, int tag)
        throws MPIException
{
    MPI.check();

    return new Intercomm(createIntercomm(handle, localComm.handle,
                                         localLeader, remoteLeader, tag));
}

private native long createIntercomm(
        long comm, long localComm, int localLeader,
        int remoteLeader, int tag) throws MPIException;

// Blocking Send and Recv

/**
 * Blocking send operation.
 * <p>Java binding of the MPI operation {@code MPI_SEND}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @throws MPIException
 */
public final void send(Object buf, int count, Datatype type, int dest, int tag)
        throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    send(buf, off, count, type, dest, tag);
}

private native void send(Object buf, int offset, int count,
                         Datatype type, int dest, int tag)
                         throws MPIException;

/**
 * Blocking receive operation.
 * <p>Java binding of the MPI operation {@code MPI_RECV}.
 * @param buf    receive buffer array
 * @param count  number of items in receive buffer
 * @param type   datatype of each item in receive buffer
 * @param source rank of source
 * @param tag    message tag
 * @return status object
 * @throws MPIException
 */
public final Status recv(Object buf, int count,
                         Datatype type, int source, int tag)
        throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    Status stat = new Status();
    recv(buf, off, count, type, source, tag, stat);
    return stat;
}

private native void recv(
        Object buf, int offset, int count, Datatype type,
        int source, int tag, Status stat) throws MPIException;

// Send-Recv

/**
 * Execute a blocking send and receive operation.
 * <p>Java binding of the MPI operation {@code MPI_SENDRECV}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param dest      rank of destination
 * @param sendtag   send tag
 * @param recvbuf   receive buffer array
 * @param recvcount number of items in receive buffer
 * @param recvtype  datatype of each item in receive buffer
 * @param source    rank of source
 * @param recvtag   receive tag
 * @return status object
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, Datatype, int, int)
 * @see mpi.Comm#recv(Object, int, Datatype, int, int)
 */
public final Status sendRecv(
    Object sendbuf, int sendcount, Datatype sendtype, int dest,   int sendtag,
    Object recvbuf, int recvcount, Datatype recvtype, int source, int recvtag)
    throws MPIException
{
    MPI.check();

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    Status stat = new Status();
    
    sendRecv(sendbuf, sendoff, sendcount, sendtype, dest, sendtag,
             recvbuf, recvoff, recvcount, recvtype, source, recvtag, stat);

    return stat;
}

private native void sendRecv(Object sbuf, int soffset, int scount,
                             Datatype stype, int dest, int stag,
                             Object rbuf, int roffset, int rcount,
                             Datatype rtype, int source, int rtag,
                             Status stat) throws MPIException;

/**
 * Execute a blocking send and receive operation,
 * receiving message into send buffer.
 * <p>Java binding of the MPI operation {@code MPI_SENDRECV_REPLACE}.
 * @param buf     buffer array
 * @param count   number of items to send
 * @param type    datatype of each item in buffer
 * @param dest    rank of destination
 * @param sendtag send tag
 * @param source  rank of source
 * @param recvtag receive tag
 * @return status object
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, Datatype, int, int)
 * @see mpi.Comm#recv(Object, int, Datatype, int, int)
 */
public final Status sendRecvReplace(
        Object buf, int count, Datatype type,
        int dest, int sendtag, int source, int recvtag)
    throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    Status stat = new Status();

    sendRecvReplace(buf, off, count, type, dest,
                    sendtag, source, recvtag, stat);
    return stat;
}

private native void sendRecvReplace(
        Object buf, int offset, int count, Datatype type, int dest,
        int stag, int source, int rtag, Status stat) throws MPIException;

// Communication Modes

/**
 * Send in buffered mode.
 * <p>Java binding of the MPI operation {@code MPI_BSEND}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final void bSend(Object buf, int count, Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    bSend(buf, off, count, type, dest, tag);
}

private native void bSend(Object buf, int offset, int count,
                          Datatype type, int dest, int tag)
                          throws MPIException;
/**
 * Send in synchronous mode.
 * <p>Java binding of the MPI operation {@code MPI_SSEND}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final void sSend(Object buf, int count, Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    sSend(buf, off, count, type, dest, tag);
}

private native void sSend(Object buf, int offset, int count,
                          Datatype type, int dest, int tag)
                          throws MPIException;

/**
 * Send in ready mode.
 * <p>Java binding of the MPI operation {@code MPI_RSEND}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final void rSend(Object buf, int count, Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    rSend(buf, off, count, type, dest, tag);
}

private native void rSend(Object buf, int offset, int count,
                          Datatype type, int dest, int tag)
                          throws MPIException;

// Nonblocking communication

/**
 * Start a standard mode, nonblocking send.
 * <p>Java binding of the MPI operation {@code MPI_ISEND}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @return communication request
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final Request iSend(Buffer buf, int count,
                           Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Request(iSend(handle, buf, count, type.handle, dest, tag));
}

/**
 * Protected member used internally by <tt>Prequest.start</tt>
 */
protected final native long iSend(
        long comm, Buffer buf, int count, long type, int dest, int tag)
        throws MPIException;

/**
 * Start a buffered mode, nonblocking send.
 * <p>Java binding of the MPI operation <tt>MPI_IBSEND</tt>.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @return communication request
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final Request ibSend(Buffer buf, int count,
                            Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Request(ibSend(handle, buf, count, type.handle, dest, tag));
}

/**
 * Protected member used internally by <tt>Prequest.Start</tt>
 */
protected final native long ibSend(
        long comm, Buffer buf, int count, long type, int dest, int tag)
        throws MPIException;

/**
 * Start a synchronous mode, nonblocking send.
 * <p>Java binding of the MPI operation {@code MPI_ISSEND}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @return communication request
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final Request isSend(Buffer buf, int count,
                            Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Request(isSend(handle, buf, count, type.handle, dest, tag));
}

/**
 * Protected member used internally by <tt>Prequest.Start</tt>
 */
protected final native long isSend(
        long comm, Buffer buf, int count, long type, int dest, int tag)
        throws MPIException;

/**
 * Start a ready mode, nonblocking send.
 * <p>Java binding of the MPI operation {@code MPI_IRSEND}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @return communication request
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final Request irSend(Buffer buf, int count,
                            Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Request(irSend(handle, buf, count, type.handle, dest, tag));
}

/**
 * Protected member used internally by <tt>Prequest.Start</tt>
 */
protected final native long irSend(
        long comm, Buffer buf, int count, long type, int dest, int tag)
        throws MPIException;

/**
 * Start a nonblocking receive.
 * <p>Java binding of the MPI operation {@code MPI_IRECV}.
 * @param buf    receive buffer array
 * @param count  number of items in receive buffer
 * @param type   datatype of each item in receive buffer
 * @param source rank of source
 * @param tag    message tag
 * @return communication request
 * @throws MPIException
 * @see mpi.Comm#recv(Object, int, Datatype, int, int)
 */
public final Request iRecv(Buffer buf, int count,
                           Datatype type, int source, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Request(iRecv(handle, buf, count, type.handle, source, tag));
}

/**
 * Protected member used internally by <tt>Prequest.Start</tt>
 */
protected final native long iRecv(
        long comm, Buffer buf, int count, long type, int source, int tag)
        throws MPIException;


// Persistent communication  requests

/**
 * Creates a persistent communication request for a standard mode send.
 * <p>Java binding of the MPI operation {@code MPI_SEND_INIT}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @return persistent communication request
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final Prequest sendInit(Buffer buf, int count,
                               Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Prequest(sendInit(handle, buf, count, type.handle, dest, tag));
}

private native long sendInit(
        long comm, Buffer buf, int count, long type, int dest, int tag)
        throws MPIException;

/**
 * Creates a persistent communication request for a buffered mode send.
 * <p>Java binding of the MPI operation {@code MPI_BSEND_INIT}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @return persistent communication request
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final Prequest bSendInit(Buffer buf, int count,
                                Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Prequest(bSendInit(handle, buf, count, type.handle, dest, tag));
}

private native long bSendInit(
        long comm, Buffer buf, int count, long type, int dest, int tag)
        throws MPIException;

/**
 * Creates a persistent communication request for a synchronous mode send.
 * <p>Java binding of the MPI operation {@code MPI_SSEND_INIT}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @return persistent communication request
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final Prequest sSendInit(Buffer buf, int count,
                                Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Prequest(sSendInit(handle, buf, count, type.handle, dest, tag));
}

private native long sSendInit(
        long comm, Buffer buf, int count, long type, int dest, int tag)
        throws MPIException;

/**
 * Creates a persistent communication request for a ready mode send.
 * <p>Java binding of the MPI operation {@code MPI_RSEND_INIT}.
 * @param buf   send buffer array
 * @param count number of items to send
 * @param type  datatype of each item in send buffer
 * @param dest  rank of destination
 * @param tag   message tag
 * @return persistent communication request
 * @throws MPIException
 * @see mpi.Comm#send(Object, int, int, Datatype, int, int)
 */
public final Prequest rSendInit(Buffer buf, int count,
                                Datatype type, int dest, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Prequest(rSendInit(handle, buf, count, type.handle, dest, tag));
}

private native long rSendInit(
        long comm, Buffer buf, int count, long type, int dest, int tag)
        throws MPIException;

/**
 * Creates a persistent communication request for a receive operation.
 * <p>Java binding of the MPI operation {@code MPI_RECV_INIT}.
 * @param buf    receive buffer array
 * @param count  number of items in receive buffer
 * @param type   datatype of each item in receive buffer
 * @param source rank of source
 * @param tag    message tag
 * @return communication request
 * @throws MPIException
 * @see mpi.Comm#recv(Object, int, Datatype, int, int)
 */
public final Prequest recvInit(Buffer buf, int count,
                               Datatype type, int source, int tag)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Prequest(recvInit(handle, buf, count, type.handle, source, tag));
}

private native long recvInit(
        long comm, Buffer buf, int count, long type, int source, int tag)
        throws MPIException;

// Pack and Unpack

/**
 * Packs message in send buffer {@code inbuf} into space specified in
 * {@code outbuf}.
 * <p>
 * Java binding of the MPI operation {@code MPI_PACK}.
 * <p>
 * The return value is the output value of {@code position} - the
 * inital value incremented by the number of bytes written.
 * @param inbuf    input buffer array
 * @param incount  number of items in input buffer
 * @param type     datatype of each item in input buffer
 * @param outbuf   output buffer
 * @param position initial position in output buffer
 * @return final position in output buffer
 * @throws MPIException
 */
public final int pack(Object inbuf, int incount, Datatype type,
                      byte[] outbuf, int position)
        throws MPIException
{
    MPI.check();
    int offset = 0;

    if(isHeapBuffer(inbuf))
    {
        offset = ((Buffer)inbuf).arrayOffset();
        inbuf  = ((Buffer)inbuf).array();
    }

    return pack(inbuf, offset, incount, type, outbuf, position);
}

private native int pack(Object inbuf, int offset, int incount,
                        Datatype type, byte[] outbuf, int position)
                        throws MPIException;

/**
 * Unpacks message in receive buffer {@code outbuf} into space specified in
 * {@code inbuf}.
 * <p>
 * Java binding of the MPI operation {@code MPI_UNPACK}.
 * <p>
 * The return value is the output value of {@code position} - the
 * inital value incremented by the number of bytes read.
 * @param inbuf    input buffer
 * @param position initial position in input buffer
 * @param outbuf   output buffer array
 * @param outcount number of items in output buffer
 * @param type     datatype of each item in output buffer
 * @return final position in input buffer
 * @throws MPIException
 */
public final int unpack(byte[] inbuf, int position,
                        Object outbuf, int outcount, Datatype type)
        throws MPIException
{
    MPI.check();
    int offset = 0;

    if(isHeapBuffer(outbuf))
    {
        offset = ((Buffer)outbuf).arrayOffset();
        outbuf = ((Buffer)outbuf).array();
    }

    return unpack(inbuf, position, outbuf, offset, outcount, type);
}

private native int unpack(byte[] inbuf, int position, Object outbuf,
                          int offset, int outcount, Datatype type)
                          throws MPIException;

/**
 * Returns an upper bound on the increment of {@code position} effected
 * by {@code pack}.
 * <p>Java binding of the MPI operation {@code MPI_PACK_SIZE}.
 * @param incount number of items in input buffer
 * @param type    datatype of each item in input buffer
 * @return upper bound on size of packed message
 * @throws MPIException
 */
public final int packSize(int incount, Datatype type) throws MPIException
{
    MPI.check();
    return packSize(handle, incount, type.handle);
}

private native int packSize(long comm, int incount, long type)
        throws MPIException;

// Probe and Cancel

/**
 * Check if there is an incoming message matching the pattern specified.
 * <p>Java binding of the MPI operation {@code MPI_IPROBE}.
 * <p>If such a message is currently available, a status object similar
 * to the return value of a matching {@code recv} operation is returned.
 * @param source rank of source
 * @param tag    message tag
 * @return status object if such a message is currently available,
 *         {@code null} otherwise.
 * @throws MPIException
 */
public final Status iProbe(int source, int tag) throws MPIException
{
    MPI.check();
    return iProbe(handle, source, tag);
}

private native Status iProbe(long comm, int source, int tag)
        throws MPIException;

/**
 * Wait until there is an incoming message matching the pattern specified.
 * <p>Java binding of the MPI operation {@code MPI_PROBE}.
 * <p>Returns a status object similar to the return value of a matching
 * {@code recv} operation.
 * @param source rank of source
 * @param tag    message tag
 * @return status object
 * @throws MPIException
 */
public final Status probe(int source, int tag) throws MPIException
{
    MPI.check();
    Status stat = new Status();
    probe(handle, source, tag, stat);
    return stat;
}

private native void probe(long comm, int source, int tag, Status stat)
        throws MPIException;

// Caching

/**
 * Create a new attribute key.
 * <p>Java binding of the MPI operation {@code MPI_COMM_CREATE_KEYVAL}.
 * @return attribute key for future access
 * @throws MPIException
 */
public static int createKeyval() throws MPIException
{
    MPI.check();
    return createKeyval_jni();
}

private static native int createKeyval_jni() throws MPIException;

/**
 * Frees an attribute key for communicators.
 * <p>Java binding of the MPI operation {@code MPI_COMM_FREE_KEYVAL}.
 * @param keyval attribute key
 * @throws MPIException
 */
public static void freeKeyval(int keyval) throws MPIException
{
    MPI.check();
    freeKeyval_jni(keyval);
}

private static native void freeKeyval_jni(int keyval) throws MPIException;

/**
 * Stores attribute value associated with a key.
 * <p>Java binding of the MPI operation {@code MPI_COMM_SET_ATTR}.
 * @param keyval attribute key
 * @param value  attribute value
 * @throws MPIException
 */
public final void setAttr(int keyval, Object value) throws MPIException
{
    MPI.check();
    setAttr(handle, keyval, MPI.attrSet(value));
}

private native void setAttr(long comm, int keyval, byte[] value)
        throws MPIException;

/**
 * Retrieves attribute value by key.
 * <p>Java binding of the MPI operation {@code MPI_COMM_GET_ATTR}.
 * @param keyval attribute key
 * @return attribute value or null if no attribute is associated with the key.
 * @throws MPIException
 */
public final Object getAttr(int keyval) throws MPIException
{
    MPI.check();

    if( keyval == MPI.TAG_UB       ||
        keyval == MPI.HOST         ||
        keyval == MPI.IO           ||
        keyval == MPI.APPNUM       ||
        keyval == MPI.LASTUSEDCODE ||
        keyval == MPI.UNIVERSE_SIZE)
    {
        return getAttr_predefined(handle, keyval);
    }
    else if(keyval == MPI.WTIME_IS_GLOBAL)
    {
        Integer value = (Integer)getAttr_predefined(handle, keyval);
        return value==null ? null : value.intValue() != 0;
    }
    else
    {
        return MPI.attrGet(getAttr(handle, keyval));
    }
}

private native Object getAttr_predefined(long comm, int keyval)
        throws MPIException;

private native byte[] getAttr(long comm, int keyval) throws MPIException;

/**
 * Deletes an attribute value associated with a key on a communicator.
 * <p>Java binding of the MPI operation {@code MPI_COMM_DELETE_ATTR}.
 * @param keyval attribute key
 * @throws MPIException
 */
public final void deleteAttr(int keyval) throws MPIException
{
    MPI.check();
    deleteAttr(handle, keyval);
}

private native void deleteAttr(long comm, int keyval) throws MPIException;

// Process Topologies

/**
 * Returns the type of topology associated with the communicator.
 * <p>Java binding of the MPI operation {@code MPI_TOPO_TEST}.
 * <p>The return value will be one of {@code MPI.GRAPH}, {@code MPI.CART}
 * or {@code MPI.UNDEFINED}.
 * @return topology type of communicator
 * @throws MPIException
 */
public final int getTopology() throws MPIException
{
    MPI.check();
    return getTopology(handle);
}

private native int getTopology(long comm) throws MPIException;

// Enviromental Management

/**
 * Abort MPI.
 * <p>Java binding of the MPI operation {@code MPI_ABORT}.
 * @param errorcode error code for Unix or POSIX environments
 * @throws MPIException
 */
public final void abort(int errorcode) throws MPIException
{
    MPI.check();
    abort(handle, errorcode);
}

private native void abort(long comm, int errorcode) throws MPIException;

// Error handler

/**
 * Associates a new error handler with communicator at the calling process.
 * <p>Java binding of the MPI operation {@code MPI_ERRHANDLER_SET}.
 * @param errhandler new MPI error handler for communicator
 * @throws MPIException
 */
public final void setErrhandler(Errhandler errhandler) throws MPIException
{
    MPI.check();
    setErrhandler(handle, errhandler.handle);
}

private native void setErrhandler(long comm, long errhandler)
        throws MPIException;

/**
 * Returns the error handler currently associated with the communicator.
 * <p>Java binding of the MPI operation {@code MPI_ERRHANDLER_GET}.
 * @return MPI error handler currently associated with communicator
 * @throws MPIException
 */
public final Errhandler getErrhandler() throws MPIException
{
    MPI.check();
    return new Errhandler(getErrhandler(handle));
}

private native long getErrhandler(long comm);

// Collective Communication

/**
 * A call to {@code barrier} blocks the caller until all process
 * in the group have called it.
 * <p>Java binding of the MPI operation {@code MPI_BARRIER}.
 * @throws MPIException
 */
public final void barrier() throws MPIException
{
    MPI.check();
    barrier(handle);
}

private native void barrier(long comm) throws MPIException;

/**
 * Nonblocking barrier sinchronization.
 * <p>Java binding of the MPI operation {@code MPI_IBARRIER}.
 * @return communication request
 * @throws MPIException
 */
public final Request iBarrier() throws MPIException
{
    MPI.check();
    return new Request(iBarrier(handle));
}

private native long iBarrier(long comm) throws MPIException;

/**
 * Broadcast a message from the process with rank {@code root}
 * to all processes of the group.
 * <p>Java binding of the MPI operation {@code MPI_BCAST}.
 * @param buf   buffer array
 * @param count number of items in buffer
 * @param type  datatype of each item in buffer
 * @param root  rank of broadcast root
 * @throws MPIException
 */
public final void bcast(Object buf, int count, Datatype type, int root)
    throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    bcast(buf, off, count, type, root);
}

private native void bcast(Object buf, int offset, int count,
                          Datatype type, int root)
                          throws MPIException;

/**
 * Broadcast a message from the process with rank {@code root}
 * to all processes of the group.
 * <p>Java binding of the MPI operation {@code MPI_IBCAST}.
 * @param buf   buffer array
 * @param count number of items in buffer
 * @param type  datatype of each item in buffer
 * @param root  rank of broadcast root
 * @return communication request
 * @throws MPIException
 */
public final Request iBcast(Buffer buf, int count, Datatype type, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Request(iBcast(handle, buf, count, type.handle, root));
}

private native long iBcast(
        long comm, Buffer buf, int count, long type, int root)
        throws MPIException;

/**
 * Each process sends the contents of its send buffer to the root process.
 * <p>Java binding of the MPI operation {@code MPI_GATHER}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of receiving process
 * @throws MPIException
 */
public final void gather(
        Object sendbuf, int sendcount, Datatype sendtype,
        Object recvbuf, int recvcount, Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    gather(sendbuf, sendoff, sendcount, sendtype,
           recvbuf, recvoff, recvcount, recvtype, root);
}

/**
 * Each process sends the contents of its send buffer to the root process.
 * <p>Java binding of the MPI operation {@code MPI_GATHER}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * The buffer is used by the root process to receive data,
 * and it is used by the non-root processes to send data.
 * @param buf   buffer array
 * @param count number of items to send/receive
 * @param type  datatype of each item in buffer
 * @param root  rank of receiving process
 * @throws MPIException
 */
public final void gather(Object buf, int count, Datatype type, int root)
    throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    gather(null, 0, 0, null, buf, off, count, type, root);
}

private native void gather(
        Object sendbuf, int sendoff, int sendcount, Datatype sendtype,
        Object recvbuf, int recvoff, int recvcount, Datatype recvtype, int root)
        throws MPIException;

/**
 * Each process sends the contents of its send buffer to the root process.
 * <p>Java binding of the MPI operation {@code MPI_IGATHER}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of receiving process
 * @return communication request
 * @throws MPIException
 */
public final Request iGather(
        Buffer sendbuf, int sendcount, Datatype sendtype,
        Buffer recvbuf, int recvcount, Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iGather(handle, sendbuf, sendcount, sendtype.handle,
                               recvbuf, recvcount, recvtype.handle, root));
}

/**
 * Each process sends the contents of its send buffer to the root process.
 * <p>Java binding of the MPI operation {@code MPI_IGATHER}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * The buffer is used by the root process to receive data,
 * and it is used by the non-root processes to send data.
 * @param buf   buffer array
 * @param count number of items to send/receive
 * @param type  datatype of each item in buffer
 * @param root  rank of receiving process
 * @return communication request
 * @throws MPIException
 */
public final Request iGather(Buffer buf, int count, Datatype type, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);

    return new Request(iGather(handle, null, 0, 0,
                               buf, count, type.handle, root));
}

private native long iGather(
        long comm, Buffer sendbuf, int sendcount, long sendtype,
        Buffer recvbuf, int recvcount, long recvtype,
        int root) throws MPIException;

/**
 * Extends functionality of {@code gather} by allowing varying
 * counts of data from each process.
 * <p>Java binding of the MPI operation {@code MPI_GATHERV}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of elements received from each process
 * @param displs    displacements at which to place incoming data
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of receiving process
 * @throws MPIException
 */
public final void gatherv(Object sendbuf, int sendcount, Datatype sendtype,
                          Object recvbuf, int[] recvcount, int[] displs,
                          Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    gatherv(sendbuf, sendoff, sendcount, sendtype,
            recvbuf, recvoff, recvcount, displs, recvtype, root);
}

/**
 * Extends functionality of {@code gather} by allowing varying
 * counts of data from each process.
 * <p>Java binding of the MPI operation {@code MPI_GATHERV} using
 * {@code MPI_IN_PLACE} instead of the send buffer in the root process.
 * This method must be used in the root process.
 * @param recvbuf   receive buffer array
 * @param recvcount number of elements received from each process
 * @param displs    displacements at which to place incoming data
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of receiving process
 * @throws MPIException
 */
public final void gatherv(Object recvbuf, int[] recvcount, int[] displs,
                          Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();
    int recvoff = 0;

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    gatherv(null, 0, 0, null,
            recvbuf, recvoff, recvcount, displs, recvtype, root);
}

/**
 * Extends functionality of {@code gather} by allowing varying
 * counts of data from each process.
 * <p>Java binding of the MPI operation {@code MPI_GATHERV} using
 * {@code MPI_IN_PLACE} instead of the send buffer in the root process.
 * This method must be used in the non-root processes.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param root      rank of receiving process
 * @throws MPIException
 */
public final void gatherv(Object sendbuf, int sendcount,
                          Datatype sendtype, int root)
    throws MPIException
{
    MPI.check();
    int sendoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    gatherv(sendbuf, sendoff, sendcount, sendtype,
            null, 0, null, null, null, root);
}

private native void gatherv(
        Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
        Object recvbuf, int recvoffset, int[] recvcount, int[] displs,
        Datatype recvtype, int root) throws MPIException;

/**
 * Extends functionality of {@code gather} by allowing varying
 * counts of data from each process.
 * <p>Java binding of the MPI operation {@code MPI_IGATHERV}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of elements received from each process
 * @param displs    displacements at which to place incoming data
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of receiving process
 * @return communication request
 * @throws MPIException
 */
public final Request iGatherv(
        Buffer sendbuf, int sendcount, Datatype sendtype, Buffer recvbuf,
        int[] recvcount, int[] displs, Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iGatherv(
            handle, sendbuf, sendcount, sendtype.handle,
            recvbuf, recvcount, displs, recvtype.handle, root));
}

/**
 * Extends functionality of {@code gather} by allowing varying
 * counts of data from each process.
 * <p>Java binding of the MPI operation {@code MPI_IGATHERV} using
 * {@code MPI_IN_PLACE} instead of the send buffer in the root process.
 * This method must be used in the root process.
 * @param recvbuf   receive buffer array
 * @param recvcount number of elements received from each process
 * @param displs    displacements at which to place incoming data
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of receiving process
 * @return communication request
 * @throws MPIException
 */
public final Request iGatherv(Buffer recvbuf, int[] recvcount, int[] displs,
                              Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(recvbuf);

    return new Request(iGatherv(handle, null, 0, 0,
            recvbuf, recvcount, displs, recvtype.handle, root));
}

/**
 * Extends functionality of {@code gather} by allowing varying
 * counts of data from each process.
 * <p>Java binding of the MPI operation {@code MPI_IGATHERV} using
 * {@code MPI_IN_PLACE} instead of the send buffer in the root process.
 * This method must be used in the non-root processes.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param root      rank of receiving process
 * @return communication request
 * @throws MPIException
 */
public final Request iGatherv(Buffer sendbuf, int sendcount,
                              Datatype sendtype, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf);

    return new Request(iGatherv(handle, sendbuf, sendcount, sendtype.handle,
                                null, null, null, 0, root));
}

private native long iGatherv(
        long handle, Buffer sendbuf, int sendcount, long sendtype,
        Buffer recvbuf, int[] recvcount, int[] displs,
        long recvtype, int root)
        throws MPIException;

/**
 * Inverse of the operation {@code gather}.
 * <p>Java binding of the MPI operation {@code MPI_SCATTER}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of sending process
 * @throws MPIException
 */
public final void scatter(
        Object sendbuf, int sendcount, Datatype sendtype,
        Object recvbuf, int recvcount, Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    scatter(sendbuf, sendoff, sendcount, sendtype,
            recvbuf, recvoff, recvcount, recvtype, root);
}

/**
 * Inverse of the operation {@code gather}.
 * <p>Java binding of the MPI operation {@code MPI_SCATTER}
 * using {@code MPI_IN_PLACE} instead of the receive buffer.
 * The buffer is used by the root process to send data,
 * and it is used by the non-root processes to receive data.
 * @param buf   send/receive buffer array
 * @param count number of items to send/receive
 * @param type  datatype of each item in buffer
 * @param root  rank of sending process
 * @throws MPIException
 */
public final void scatter(Object buf, int count, Datatype type, int root)
    throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    scatter(buf, off, count, type, null, 0, 0, null, root);
}

private native void scatter(
        Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
        Object recvbuf, int recvoffset, int recvcount, Datatype recvtype,
        int root) throws MPIException;

/**
 * Inverse of the operation {@code gather}.
 * <p>Java binding of the MPI operation {@code MPI_ISCATTER}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of sending process
 * @return communication request
 * @throws MPIException
 */
public final Request iScatter(
        Buffer sendbuf, int sendcount, Datatype sendtype,
        Buffer recvbuf, int recvcount, Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iScatter(handle, sendbuf, sendcount, sendtype.handle,
                                recvbuf, recvcount, recvtype.handle, root));
}

/**
 * Inverse of the operation {@code gather}.
 * <p>Java binding of the MPI operation {@code MPI_ISCATTER}
 * using {@code MPI_IN_PLACE} instead of the receive buffer.
 * The buffer is used by the root process to send data,
 * and it is used by the non-root processes to receive data.
 * @param buf   send/receive buffer array
 * @param count number of items to send/receive
 * @param type  datatype of each item in buffer
 * @param root  rank of sending process
 * @return communication request
 * @throws MPIException
 */
public final Request iScatter(Buffer buf, int count, Datatype type, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);

    return new Request(iScatter(handle, buf, count, type.handle,
                                null, 0, 0, root));
}

private native long iScatter(
        long comm, Buffer sendbuf, int sendcount, long sendtype,
        Buffer recvbuf, int recvcount, long recvtype, int root)
        throws MPIException;

/**
 * Inverse of the operation {@code gatherv}.
 * <p>Java binding of the MPI operation {@code MPI_SCATTERV}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items sent to each process
 * @param displs    displacements from which to take outgoing data
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of sending process
 * @throws MPIException
 */
public final void scatterv(
        Object sendbuf, int[] sendcount, int[] displs, Datatype sendtype,
        Object recvbuf, int recvcount, Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    scatterv(sendbuf, sendoff, sendcount, displs, sendtype,
             recvbuf, recvoff, recvcount, recvtype, root);
}

/**
 * Inverse of the operation {@code gatherv}.
 * <p>Java binding of the MPI operation {@code MPI_SCATTERV} using
 * {@code MPI_IN_PLACE} instead of the receive buffer in the root process.
 * This method must be used in the root process.
 * @param sendbuf   send buffer array
 * @param sendcount number of items sent to each process
 * @param displs    displacements from which to take outgoing data
 * @param sendtype  datatype of each item in send buffer
 * @param root      rank of sending process
 * @throws MPIException
 */
public final void scatterv(Object sendbuf, int[] sendcount, int[] displs,
                           Datatype sendtype, int root)
    throws MPIException
{
    MPI.check();
    int sendoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    scatterv(sendbuf, sendoff, sendcount, displs, sendtype,
             null, 0, 0, null, root);
}

/**
 * Inverse of the operation {@code gatherv}.
 * <p>Java binding of the MPI operation {@code MPI_SCATTERV} using
 * {@code MPI_IN_PLACE} instead of the receive buffer in the root process.
 * This method must be used in the non-root processes.
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of sending process
 * @throws MPIException
 */
public final void scatterv(Object recvbuf, int recvcount,
                           Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();
    int recvoff = 0;

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    scatterv(null, 0, null, null, null,
             recvbuf, recvoff, recvcount, recvtype, root);
}

private native void scatterv(
        Object sendbuf, int sendoffset,
        int[] sendcount, int[] displs, Datatype sendtype,
        Object recvbuf, int recvoffset, int recvcount,
        Datatype recvtype, int root)
        throws MPIException;

/**
 * Inverse of the operation {@code gatherv}.
 * <p>Java binding of the MPI operation {@code MPI_ISCATTERV}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items sent to each process
 * @param displs    displacements from which to take outgoing data
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of sending process
 * @return communication request
 * @throws MPIException
 */
public final Request iScatterv(
        Buffer sendbuf, int[] sendcount, int[] displs,  Datatype sendtype,
        Buffer recvbuf, int recvcount, Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iScatterv(
            handle, sendbuf, sendcount, displs, sendtype.handle,
            recvbuf, recvcount, recvtype.handle, root));
}

/**
 * Inverse of the operation {@code gatherv}.
 * <p>Java binding of the MPI operation {@code MPI_ISCATTERV} using
 * {@code MPI_IN_PLACE} instead of the receive buffer in the root process.
 * This method must be used in the root process.
 * @param sendbuf   send buffer array
 * @param sendcount number of items sent to each process
 * @param displs    displacements from which to take outgoing data
 * @param sendtype  datatype of each item in send buffer
 * @param root      rank of sending process
 * @return communication request
 * @throws MPIException
 */
public final Request iScatterv(Buffer sendbuf, int[] sendcount, int[] displs,
                               Datatype sendtype, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf);

    return new Request(iScatterv(handle, sendbuf, sendcount, displs,
                                 sendtype.handle, null, 0, 0, root));
}

/**
 * Inverse of the operation {@code gatherv}.
 * <p>Java binding of the MPI operation {@code MPI_ISCATTERV} using
 * {@code MPI_IN_PLACE} instead of the receive buffer in the root process.
 * This method must be used in the non-root processes.
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @param root      rank of sending process
 * @return communication request
 * @throws MPIException
 */
public final Request iScatterv(Buffer recvbuf, int recvcount,
                               Datatype recvtype, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(recvbuf);

    return new Request(iScatterv(handle, null, null, null, 0,
                                 recvbuf, recvcount, recvtype.handle, root));
}

private native long iScatterv(
        long comm, Buffer sendbuf, int[] sendcount, int[] displs, long sendtype,
        Buffer recvbuf, int recvcount, long recvtype, int root)
        throws MPIException;

/**
 * Similar to {@code gather}, but all processes receive the result.
 * <p>Java binding of the MPI operation {@code MPI_ALLGATHER}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @throws MPIException
 */
public final void allGather(Object sendbuf, int sendcount, Datatype sendtype,
                            Object recvbuf, int recvcount, Datatype recvtype)
    throws MPIException
{
    MPI.check();

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    allGather(sendbuf, sendoff, sendcount, sendtype,
              recvbuf, recvoff, recvcount, recvtype);
}

/**
 * Similar to {@code gather}, but all processes receive the result.
 * <p>Java binding of the MPI operation {@code MPI_ALLGATHER}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf   receive buffer array
 * @param count number of items to receive
 * @param type  datatype of each item in receive buffer
 * @throws MPIException
 */
public final void allGather(Object buf, int count, Datatype type)
    throws MPIException
{
    MPI.check();
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    allGather(null, 0, 0, null, buf, off, count, type);
}

private native void allGather(
        Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
        Object recvbuf, int recvoffset, int recvcount, Datatype recvtype)
        throws MPIException;

/**
 * Similar to {@code gather}, but all processes receive the result.
 * <p>Java binding of the MPI operation {@code MPI_IALLGATHER}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of items to receive
 * @param recvtype  datatype of each item in receive buffer
 * @return communication request
 * @throws MPIException
 */
public final Request iAllGather(
        Buffer sendbuf, int sendcount, Datatype sendtype,
        Buffer recvbuf, int recvcount, Datatype recvtype)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iAllGather(handle, sendbuf, sendcount, sendtype.handle,
                                  recvbuf, recvcount, recvtype.handle));
}

/**
 * Similar to {@code gather}, but all processes receive the result.
 * <p>Java binding of the MPI operation {@code MPI_IALLGATHER}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf   receive buffer array
 * @param count number of items to receive
 * @param type  datatype of each item in receive buffer
 * @return communication request
 * @throws MPIException
 */
public final Request iAllGather(Buffer buf, int count, Datatype type)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    return new Request(iAllGather(handle, null, 0, 0, buf, count, type.handle));
}

private native long iAllGather(
        long comm, Buffer sendbuf, int sendcount, long sendtype,
        Buffer recvbuf, int recvcount, long recvtype) throws MPIException;

/**
 * Similar to {@code gatherv}, but all processes receive the result.
 * <p>Java binding of the MPI operation {@code MPI_ALLGATHERV}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of elements received from each process
 * @param displs    displacements at which to place incoming data
 * @param recvtype  datatype of each item in receive buffer
 * @throws MPIException
 */
public final void allGatherv(
        Object sendbuf, int sendcount, Datatype sendtype,
        Object recvbuf, int[] recvcount, int[] displs, Datatype recvtype)
    throws MPIException
{
    MPI.check();

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    allGatherv(sendbuf, sendoff, sendcount, sendtype,
               recvbuf, recvoff, recvcount, displs, recvtype);
}

/**
 * Similar to {@code gatherv}, but all processes receive the result.
 * <p>Java binding of the MPI operation {@code MPI_ALLGATHERV}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param recvbuf   receive buffer array
 * @param recvcount number of elements received from each process
 * @param displs    displacements at which to place incoming data
 * @param recvtype  datatype of each item in receive buffer
 * @throws MPIException
 */
public final void allGatherv(Object recvbuf, int[] recvcount,
                             int[] displs, Datatype recvtype)
    throws MPIException
{
    MPI.check();
    int recvoff = 0;

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    allGatherv(null, 0, 0, null, recvbuf, recvoff, recvcount, displs, recvtype);
}

private native void allGatherv(
        Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
        Object recvbuf, int recvoffset, int[] recvcount, int[] displs,
        Datatype recvtype) throws MPIException;

/**
 * Similar to {@code gatherv}, but all processes receive the result.
 * <p>Java binding of the MPI operation {@code MPI_IALLGATHERV}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items to send
 * @param sendtype  datatype of each item in send buffer
 * @param recvbuf   receive buffer array
 * @param recvcount number of elements received from each process
 * @param displs    displacements at which to place incoming data
 * @param recvtype  datatype of each item in receive buffer
 * @return communication request
 * @throws MPIException
 */
public final Request iAllGatherv(
        Buffer sendbuf, int sendcount, Datatype sendtype,
        Buffer recvbuf, int[] recvcount, int[] displs, Datatype recvtype)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iAllGatherv(
            handle, sendbuf, sendcount, sendtype.handle,
            recvbuf, recvcount, displs, recvtype.handle));
}

/**
 * Similar to {@code gatherv}, but all processes receive the result.
 * <p>Java binding of the MPI operation {@code MPI_IALLGATHERV}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf    receive buffer array
 * @param count  number of elements received from each process
 * @param displs displacements at which to place incoming data
 * @param type   datatype of each item in receive buffer
 * @return communication request
 * @throws MPIException
 */
public final Request iAllGatherv(
        Buffer buf, int[] count, int[] displs, Datatype type)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);

    return new Request(iAllGatherv(
            handle, null, 0, 0, buf, count, displs, type.handle));
}

private native long iAllGatherv(
        long handle, Buffer sendbuf, int sendcount, long sendtype,
        Buffer recvbuf, int[] recvcount, int[] displs, long recvtype)
        throws MPIException;

/**
 * Extension of {@code allGather} to the case where each process sends
 * distinct data to each of the receivers.
 * <p>Java binding of the MPI operation {@code MPI_ALLTOALL}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items sent to each process
 * @param sendtype  datatype send buffer items
 * @param recvbuf   receive buffer array
 * @param recvcount number of items received from any process
 * @param recvtype  datatype of receive buffer items
 * @throws MPIException
 */
public final void allToAll(Object sendbuf, int sendcount, Datatype sendtype,
                           Object recvbuf, int recvcount, Datatype recvtype)
    throws MPIException
{
    MPI.check();

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    allToAll(sendbuf, sendoff, sendcount, sendtype,
             recvbuf, recvoff, recvcount, recvtype);
}

private native void allToAll(
        Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
        Object recvbuf, int recvoffset, int recvcount, Datatype recvtype)
        throws MPIException;

/**
 * Extension of {@code allGather} to the case where each process sends
 * distinct data to each of the receivers.
 * <p>Java binding of the MPI operation {@code MPI_IALLTOALL}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items sent to each process
 * @param sendtype  datatype send buffer items
 * @param recvbuf   receive buffer array
 * @param recvcount number of items received from any process
 * @param recvtype  datatype of receive buffer items
 * @return communication request
 * @throws MPIException
 */
public final Request iAllToAll(Buffer sendbuf, int sendcount, Datatype sendtype,
                               Buffer recvbuf, int recvcount, Datatype recvtype)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iAllToAll(handle, sendbuf, sendcount, sendtype.handle,
                                 recvbuf, recvcount, recvtype.handle));
}

private native long iAllToAll(
        long comm, Buffer sendbuf, int sendcount, long sendtype,
        Buffer recvbuf, int recvcount, long recvtype) throws MPIException;

/**
 * Adds flexibility to {@code allToAll}: location of data for send is
 * specified by {@code sdispls} and location to place data on receive
 * side is specified by {@code rdispls}.
 * <p>Java binding of the MPI operation {@code MPI_ALLTOALLV}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items sent to each buffer
 * @param sdispls   displacements from which to take outgoing data
 * @param sendtype  datatype send buffer items
 * @param recvbuf   receive buffer array
 * @param recvcount number of elements received from each process
 * @param rdispls   displacements at which to place incoming data
 * @param recvtype  datatype of each item in receive buffer
 * @throws MPIException
 */
public final void allToAllv(
        Object sendbuf, int[] sendcount, int[] sdispls, Datatype sendtype,
        Object recvbuf, int[] recvcount, int[] rdispls, Datatype recvtype)
    throws MPIException
{
    MPI.check();

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    allToAllv(sendbuf, sendoff, sendcount, sdispls, sendtype,
              recvbuf, recvoff, recvcount, rdispls, recvtype);
}

private native void allToAllv(
        Object sendbuf, int sendoffset,
        int[] sendcount, int[] sdispls, Datatype sendtype,
        Object recvbuf, int recvoffset,
        int[] recvcount, int[] rdispls, Datatype recvtype)
        throws MPIException;

/**
 * Adds flexibility to {@code allToAll}: location of data for send is
 * specified by {@code sdispls} and location to place data on receive
 * side is specified by {@code rdispls}.
 * <p>Java binding of the MPI operation {@code MPI_IALLTOALLV}.
 * @param sendbuf   send buffer array
 * @param sendcount number of items sent to each buffer
 * @param sdispls   displacements from which to take outgoing data
 * @param sendtype  datatype send buffer items
 * @param recvbuf   receive buffer array
 * @param recvcount number of elements received from each process
 * @param rdispls   displacements at which to place incoming data
 * @param recvtype  datatype of each item in receive buffer
 * @return communication request
 * @throws MPIException
 */
public final Request iAllToAllv(
        Buffer sendbuf, int[] sendcount, int[] sdispls, Datatype sendtype,
        Buffer recvbuf, int[] recvcount, int[] rdispls, Datatype recvtype)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iAllToAllv(
            handle, sendbuf, sendcount, sdispls, sendtype.handle,
            recvbuf, recvcount, rdispls, recvtype.handle));
}

private native long iAllToAllv(long comm,
        Buffer sendbuf, int[] sendcount, int[] sdispls, long sendtype,
        Buffer recvbuf, int[] recvcount, int[] rdispls, long recvtype)
        throws MPIException;

/**
 * Combine elements in input buffer of each process using the reduce
 * operation, and return the combined value in the output buffer of the
 * root process.
 * <p>
 * Java binding of the MPI operation {@code MPI_REDUCE}.
 * <p>
 * The predefined operations are available in Java as {@code MPI.MAX},
 * {@code MPI.MIN}, {@code MPI.SUM}, {@code MPI.PROD}, {@code MPI.LAND},
 * {@code MPI.BAND}, {@code MPI.LOR}, {@code MPI.BOR}, {@code MPI.LXOR},
 * {@code MPI.BXOR}, {@code MPI.MINLOC} and {@code MPI.MAXLOC}.
 * @param sendbuf send buffer array
 * @param recvbuf receive buffer array
 * @param count   number of items in send buffer
 * @param type    data type of each item in send buffer
 * @param op      reduce operation
 * @param root    rank of root process
 * @throws MPIException
 */
public final void reduce(Object sendbuf, Object recvbuf, int count,
                         Datatype type, Op op, int root)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    reduce(sendbuf, sendoff, recvbuf, recvoff, count, type, op, root);
}

/**
 * Combine elements in input buffer of each process using the reduce
 * operation, and return the combined value in the output buffer of the
 * root process.
 * <p>Java binding of the MPI operation {@code MPI_REDUCE}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf   send/receive buffer array
 * @param count number of items in buffer
 * @param type  data type of each item in buffer
 * @param op    reduce operation
 * @param root  rank of root process
 * @throws MPIException
 */
public final void reduce(Object buf, int count, Datatype type, Op op, int root)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    reduce(null, 0, buf, off, count, type, op, root);
}

private native void reduce(
        Object sendbuf, int sendoff, Object recvbuf, int recvoff,
        int count, Datatype type, Op op, int root) throws MPIException;

/**
 * Combine elements in input buffer of each process using the reduce
 * operation, and return the combined value in the output buffer of the
 * root process.
 * <p>Java binding of the MPI operation {@code MPI_IREDUCE}.
 * @param sendbuf send buffer array
 * @param recvbuf receive buffer array
 * @param count   number of items in send buffer
 * @param type    data type of each item in send buffer
 * @param op      reduce operation
 * @param root    rank of root process
 * @return communication request
 * @throws MPIException
 */
public final Request iReduce(Buffer sendbuf, Buffer recvbuf,
                             int count, Datatype type, Op op, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);
    op.setDatatype(type);

    return new Request(iReduce(handle, sendbuf, recvbuf, count,
                               type.handle, type.baseType, op, root));
}

/**
 * Combine elements in input buffer of each process using the reduce
 * operation, and return the combined value in the output buffer of the
 * root process.
 * <p>Java binding of the MPI operation {@code MPI_IREDUCE}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf   send/receive buffer array
 * @param count number of items in buffer
 * @param type  data type of each item in buffer
 * @param op    reduce operation
 * @param root  rank of root process
 * @return communication request
 * @throws MPIException
 */
public final Request iReduce(Buffer buf, int count,
                             Datatype type, Op op, int root)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(buf);
    op.setDatatype(type);

    return new Request(iReduce(handle, null, buf, count,
                               type.handle, type.baseType, op, root));
}

private native long iReduce(long comm, Buffer sendbuf, Buffer recvbuf,
                            int count, long type, int baseType, Op op, int root)
                            throws MPIException;

/**
 * Same as {@code reduce} except that the result appears in receive
 * buffer of all process in the group.
 * <p>Java binding of the MPI operation {@code MPI_ALLREDUCE}.
 * @param sendbuf send buffer array
 * @param recvbuf receive buffer array
 * @param count   number of items in send buffer
 * @param type    data type of each item in send buffer
 * @param op      reduce operation
 * @throws MPIException
 */
public final void allReduce(Object sendbuf, Object recvbuf,
                            int count, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    allReduce(sendbuf, sendoff, recvbuf, recvoff, count, type, op);
}

/**
 * Same as {@code reduce} except that the result appears in receive
 * buffer of all process in the group.
 * <p>Java binding of the MPI operation {@code MPI_ALLREDUCE}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf   receive buffer array
 * @param count number of items in send buffer
 * @param type  data type of each item in send buffer
 * @param op    reduce operation
 * @throws MPIException
 */
public final void allReduce(Object buf, int count, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    allReduce(null, 0, buf, off, count, type, op);
}

private native void allReduce(
        Object sendbuf, int sendoff, Object recvbuf, int recvoff,
        int count, Datatype type, Op op) throws MPIException;

/**
 * Same as {@code reduce} except that the result appears in receive
 * buffer of all process in the group.
 * <p>Java binding of the MPI operation {@code MPI_IALLREDUCE}.
 * @param sendbuf send buffer array
 * @param recvbuf receive buffer array
 * @param count   number of items in send buffer
 * @param type    data type of each item in send buffer
 * @param op      reduce operation
 * @return communication request
 * @throws MPIException
 */
public final Request iAllReduce(Buffer sendbuf, Buffer recvbuf,
                                int count, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    assertDirectBuffer(sendbuf, recvbuf);
    op.setDatatype(type);

    return new Request(iAllReduce(handle, sendbuf, recvbuf, count,
                                  type.handle, type.baseType, op));
}

/**
 * Same as {@code reduce} except that the result appears in receive
 * buffer of all process in the group.
 * <p>Java binding of the MPI operation {@code MPI_IALLREDUCE}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf   receive buffer array
 * @param count number of items in send buffer
 * @param type  data type of each item in send buffer
 * @param op    reduce operation
 * @return communication request
 * @throws MPIException
 */
public final Request iAllReduce(Buffer buf, int count, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    assertDirectBuffer(buf);

    return new Request(iAllReduce(handle, null, buf, count,
                                  type.handle, type.baseType, op));
}

private native long iAllReduce(
        long comm, Buffer sendbuf, Buffer recvbuf, int count,
        long type, int baseType, Op op) throws MPIException;

/**
 * Combine elements in input buffer of each process using the reduce
 * operation, and scatter the combined values over the output buffers
 * of the processes.
 * <p>Java binding of the MPI operation {@code MPI_REDUCE_SCATTER}.
 * @param sendbuf    send buffer array
 * @param recvbuf    receive buffer array
 * @param recvcounts numbers of result elements distributed to each process
 * @param type       data type of each item in send buffer
 * @param op         reduce operation
 * @throws MPIException
 */
public final void reduceScatter(Object sendbuf, Object recvbuf,
                                int[] recvcounts, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    reduceScatter(sendbuf, sendoff, recvbuf, recvoff, recvcounts, type, op);
}

/**
 * Combine elements in input buffer of each process using the reduce
 * operation, and scatter the combined values over the output buffers
 * of the processes.
 * <p>Java binding of the MPI operation {@code MPI_REDUCE_SCATTER}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf    receive buffer array
 * @param counts numbers of result elements distributed to each process
 * @param type   data type of each item in send buffer
 * @param op     reduce operation
 * @throws MPIException
 */
public final void reduceScatter(Object buf, int[] counts, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    reduceScatter(null, 0, buf, off, counts, type, op);
}

private native void reduceScatter(
        Object sendbuf, int sendoff, Object recvbuf, int recvoff,
        int[] recvcounts, Datatype type, Op op) throws MPIException;

/**
 * Combine elements in input buffer of each process using the reduce
 * operation, and scatter the combined values over the output buffers
 * of the processes.
 * <p>Java binding of the MPI operation {@code MPI_IREDUCE_SCATTER}.
 * @param sendbuf    send buffer array
 * @param recvbuf    receive buffer array
 * @param recvcounts numbers of result elements distributed to each process
 * @param type       data type of each item in send buffer
 * @param op         reduce operation
 * @return communication request
 * @throws MPIException
 */
public final Request iReduceScatter(Buffer sendbuf, Buffer recvbuf,
                                    int[] recvcounts, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iReduceScatter(handle, sendbuf, recvbuf, recvcounts,
                                      type.handle, type.baseType, op));
}

/**
 * Combine elements in input buffer of each process using the reduce
 * operation, and scatter the combined values over the output buffers
 * of the processes.
 * <p>Java binding of the MPI operation {@code MPI_IREDUCE_SCATTER}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf    receive buffer array
 * @param counts numbers of result elements distributed to each process
 * @param type   data type of each item in send buffer
 * @param op     reduce operation
 * @return communication request
 * @throws MPIException
 */
public final Request iReduceScatter(
        Buffer buf, int[] counts, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    assertDirectBuffer(buf);

    return new Request(iReduceScatter(handle, null, buf, counts,
                                      type.handle, type.baseType, op));
}

private native long iReduceScatter(
        long handle, Buffer sendbuf, Object recvbuf, int[] recvcounts,
        long type, int baseType, Op op) throws MPIException;

/**
 * Combine values and scatter the results.
 * <p>Java binding of the MPI operation {@code MPI_REDUCE_SCATTER_BLOCK}.
 * @param sendbuf   send buffer array
 * @param recvbuf   receive buffer array
 * @param recvcount element count per block
 * @param type      data type of each item in send buffer
 * @param op        reduce operation
 * @throws MPIException
 */
public final void reduceScatterBlock(Object sendbuf, Object recvbuf,
                                     int recvcount, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);

    int sendoff = 0,
        recvoff = 0;

    if(isHeapBuffer(sendbuf))
    {
        sendoff = ((Buffer)sendbuf).arrayOffset();
        sendbuf = ((Buffer)sendbuf).array();
    }

    if(isHeapBuffer(recvbuf))
    {
        recvoff = ((Buffer)recvbuf).arrayOffset();
        recvbuf = ((Buffer)recvbuf).array();
    }

    reduceScatterBlock(sendbuf, sendoff, recvbuf, recvoff, recvcount, type, op);
}

/**
 * Combine values and scatter the results.
 * <p>Java binding of the MPI operation {@code MPI_REDUCE_SCATTER_BLOCK}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf   receive buffer array
 * @param count element count per block
 * @param type  data type of each item in send buffer
 * @param op    reduce operation
 * @throws MPIException
 */
public final void reduceScatterBlock(
        Object buf, int count, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    int off = 0;

    if(isHeapBuffer(buf))
    {
        off = ((Buffer)buf).arrayOffset();
        buf = ((Buffer)buf).array();
    }

    reduceScatterBlock(null, 0, buf, off, count, type, op);
}

private native void reduceScatterBlock(
        Object sendbuf, int sendoffset, Object recvbuf, int recvoffset,
        int recvcount, Datatype type, Op op) throws MPIException;

/**
 * Combine values and scatter the results.
 * <p>Java binding of the MPI operation {@code MPI_IREDUCE_SCATTER_BLOCK}.
 * @param sendbuf   send buffer array
 * @param recvbuf   receive buffer array
 * @param recvcount element count per block
 * @param type      data type of each item in send buffer
 * @param op        reduce operation
 * @return communication request
 * @throws MPIException
 */
public final Request iReduceScatterBlock(
        Buffer sendbuf, Buffer recvbuf, int recvcount, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    assertDirectBuffer(sendbuf, recvbuf);

    return new Request(iReduceScatterBlock(handle, sendbuf, recvbuf, recvcount,
                                           type.handle, type.baseType, op));
}

/**
 * Combine values and scatter the results.
 * <p>Java binding of the MPI operation {@code MPI_IREDUCE_SCATTER_BLOCK}
 * using {@code MPI_IN_PLACE} instead of the send buffer.
 * @param buf   receive buffer array
 * @param count element count per block
 * @param type  data type of each item in send buffer
 * @param op    reduce operation
 * @return communication request
 * @throws MPIException
 */
public final Request iReduceScatterBlock(
        Buffer buf, int count, Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    assertDirectBuffer(buf);

    return new Request(iReduceScatterBlock(handle, null, buf, count,
                                           type.handle, type.baseType, op));
}

private native long iReduceScatterBlock(
        long handle, Buffer sendbuf, Buffer recvbuf, int recvcount,
        long type, int baseType, Op op) throws MPIException;

/**
 * Apply the operation given by {@code op} element-wise to the
 * elements of {@code inbuf} and {@code inoutbuf} with the result
 * stored element-wise in {@code inoutbuf}.
 * <p>Java binding of the MPI operation {@code MPI_REDUCE_LOCAL}.
 * @param inbuf    input buffer array
 * @param inoutbuf input buffer array, will contain combined output
 * @param count    number of elements
 * @param type     data type of each item
 * @param op       reduce operation
 * @throws MPIException
 */
public final void reduceLocal(Object inbuf, Object inoutbuf, int count,
                              Datatype type, Op op)
    throws MPIException
{
    MPI.check();
    op.setDatatype(type);
    reduceLocal_jni(inbuf, inoutbuf, count, type, op);
}

private native void reduceLocal_jni(Object inbuf, Object inoutbuf, int count,
                                    Datatype datatype, Op op)
                                    throws MPIException;

/**
 * Sets the print name for the communicator.
 * @param name name for the communicator
 * @throws MPIException
 */
public final void setName(String name) throws MPIException
{
    MPI.check();
    setName(handle, name);
}

private native void setName(long handle, String name) throws MPIException;

/**
 * Return the print name from the communicator.
 * @return name of the communicator
 * @throws MPIException
 */
public final String getName() throws MPIException
{
    MPI.check();
    return getName(handle);
}

private native String getName(long handle) throws MPIException;

} // Comm
