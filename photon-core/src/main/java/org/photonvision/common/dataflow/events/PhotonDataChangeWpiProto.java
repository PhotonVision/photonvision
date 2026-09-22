package org.photonvision.common.dataflow.events;
import photonvision.core.proto.PhotonMessage.PhotonDataChangeEvent;
import us.hebi.quickbuf.Descriptors.Descriptor;

import org.wpilib.util.protobuf.Protobuf;

public class PhotonDataChangeWpiProto implements Protobuf<PhotonDataChangeEvent, PhotonDataChangeEvent> {
public static final PhotonDataChangeWpiProto INSTANCE = new PhotonDataChangeWpiProto();

      @Override
      public Class<PhotonDataChangeEvent> getTypeClass() {
        return PhotonDataChangeEvent.class;
      }

      @Override
      public Descriptor getDescriptor() {
        return PhotonDataChangeEvent.getDescriptor();
      }

      @Override
      public PhotonDataChangeEvent createMessage() {
        return PhotonDataChangeEvent.newInstance();
      }

      @Override
      public PhotonDataChangeEvent unpack(PhotonDataChangeEvent msg) {
        return msg.clone();
      }

      @Override
      public void pack(PhotonDataChangeEvent msg, PhotonDataChangeEvent value) {
        msg.copyFrom(value);
      }

      @Override
      public void unpackInto(
          PhotonDataChangeEvent out, PhotonDataChangeEvent msg) {
        out.copyFrom(msg);
      }

      @Override
      public boolean isCloneable() {
        return true;
      }

      @Override
      public PhotonDataChangeEvent clone(PhotonDataChangeEvent obj) {
        return obj.clone();
      }
}
