const {onCall,HttpsError}=require('firebase-functions/v2/https');
const {initializeApp}=require('firebase-admin/app');
const {getFirestore,FieldValue}=require('firebase-admin/firestore');
initializeApp();
const db=getFirestore();
// Live confirmation, including slot capacity, is server-side and atomic.
exports.confirmDemoBooking=onCall({enforceAppCheck:true,maxInstances:10},async(req)=>{
  if(!req.auth)throw new HttpsError('unauthenticated','Sign in required');
  const role=(await db.collection('roles').doc(req.auth.uid).get()).data()?.role;
  if(role!=='admin')throw new HttpsError('permission-denied','Administrator access required');
  const bookingId=req.data?.bookingId;
  if(typeof bookingId!=='string'||!/^.{5,150}$/.test(bookingId))throw new HttpsError('invalid-argument','Invalid booking reference');
  return await db.runTransaction(async(tx)=>{
    const ref=db.collection('demoBookings').doc(bookingId);
    const booking=await tx.get(ref);
    if(!booking.exists)throw new HttpsError('not-found','Booking request missing');
    if(booking.data().status==='confirmed')return {status:'confirmed'};
    if(booking.data().status!=='requested')throw new HttpsError('failed-precondition','Request not pending');
    const slotRef=db.collection('demoSlots').doc(booking.data().slotId);
    const slot=await tx.get(slotRef);
    if(!slot.exists||slot.data().active!==true)throw new HttpsError('failed-precondition','Slot unavailable');
    if(new Date(slot.data().startAt).getTime()<=Date.now())throw new HttpsError('failed-precondition','Slot has passed');
    const booked=slot.data().bookedCount||0;
    if(!Number.isInteger(slot.data().capacity)||booked>=slot.data().capacity)throw new HttpsError('resource-exhausted','Slot is full');
    tx.update(slotRef,{bookedCount:FieldValue.increment(1)});
    tx.update(ref,{status:'confirmed',confirmedAt:FieldValue.serverTimestamp()});
    return {status:'confirmed'};
  });
});
