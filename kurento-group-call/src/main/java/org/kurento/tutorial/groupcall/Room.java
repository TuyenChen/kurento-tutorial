/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.tutorial.groupcall;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.3.1
 */
public class Room implements Closeable {
  private final Logger log = LoggerFactory.getLogger(Room.class);

  private final ConcurrentMap<String, UserSession> participants = new ConcurrentHashMap<>();
  private final MediaPipeline pipeline;
  private final String name;
  public UserSession teacher;
  public UserSession speaker;

  public String getName() {
    return name;
  }

  public Room(String roomName, MediaPipeline pipeline) {
    this.name = roomName;
    this.pipeline = pipeline;
    log.info("ROOM {} has been created", roomName);
  }

  @PreDestroy
  private void shutdown() {
    this.close();
  }

  public UserSession join(String userName, String role, WebSocketSession session) throws IOException {
    log.info("ROOM {}: adding participant {}", userName, userName);
    //first participant is teacher
    
    if(role.equals("teacher")) {
      teacher = new UserSession(userName, role, this.name,session, this.pipeline);
      serverCongNhanLateacher();//Gui danh sach participants da~ o? trong room
      //Thong bao teacher vao` cho moi ng
      for (final UserSession participant : this.getParticipants()) {
        sendTeacherName(participant);
      }
      return teacher;
    }else{
      final UserSession participant = new UserSession(userName, role, this.name, session, this.pipeline);
      participants.put(participant.getName(), participant);
      
      joinRoom(participant);
    
      sendParticipantNames(participant);
      if (teacher != null){
        sendTeacherName(participant);
      }
      return participant;
    }
    
  }

  public void leave(UserSession user) throws IOException {
    log.debug("PARTICIPANT {}: Leaving room {}", user.getName(), this.name);
    if(user != null){
      this.removeParticipant(user.getName());
      user.close();
    }
  }

  private Collection<String> joinRoom(UserSession newParticipant) throws IOException {
    final JsonObject newParticipantMsg = new JsonObject();
    newParticipantMsg.addProperty("id", "newParticipantArrived");
    newParticipantMsg.addProperty("name", newParticipant.getName());

    final List<String> participantsList = new ArrayList<>(participants.values().size());
    log.debug("ROOM {}: notifying other participants of new participant {}", name,
        newParticipant.getName());
    log.info("Send message New participant: {} to all members", newParticipant.getName());
    for (final UserSession participant : participants.values()) {
      try {
        participant.sendMessage(newParticipantMsg);
      } catch (final IOException e) {
        log.debug("ROOM {}: participant {} could not be notified", name, participant.getName(), e);
      }
      participantsList.add(participant.getName());
    }
    if (teacher != null){
      teacher.sendMessage(newParticipantMsg);
    }

    return participantsList;
  }

  private void removeParticipant(String name) throws IOException {
    
    if (teacher!=null && name.equals(teacher.getName())) {
      this.teacher = null;
      final JsonObject teacherLeftJson = new JsonObject();
      teacherLeftJson.addProperty("id", "teacherLeft");
      teacherLeftJson.addProperty("name", name);
      for (final UserSession participant : participants.values()) {
        try {
          participant.cancelVideoFrom(name);
          participant.sendMessage(teacherLeftJson);
        } catch (final IOException e) {
          // unnotifiedParticipants.add(participant.getName());
        }
      }
    }else {
      participants.remove(name);
      log.debug("ROOM {}: notifying all users that {} is leaving the room", this.name, name);

      final List<String> unnotifiedParticipants = new ArrayList<>();
      final JsonObject participantLeftJson = new JsonObject();
      participantLeftJson.addProperty("id", "participantLeft");
      participantLeftJson.addProperty("name", name);
      for (final UserSession participant : participants.values()) {
        try {
          // participant.cancelVideoFrom(name);
          participant.sendMessage(participantLeftJson);
        } catch (final IOException e) {
          unnotifiedParticipants.add(participant.getName());
        }
      }
      if (teacher!=null){
        teacher.sendMessage(participantLeftJson);
      }
      if (!unnotifiedParticipants.isEmpty()) {
        log.debug("ROOM {}: The users {} could not be notified that {} left the room", this.name,
            unnotifiedParticipants, name);
      }
    }

  }

  public void serverCongNhanLateacher() throws IOException {
    log.info("Thong bao {} la` teacher",teacher.getName());
    final JsonArray participantsArray = new JsonArray();
    for (final UserSession participant : this.getParticipants()) {
        final JsonElement participantName = new JsonPrimitive(participant.getName());
        participantsArray.add(participantName);
    }

    final JsonObject existingParticipants4TMsg = new JsonObject();
    existingParticipants4TMsg.addProperty("id", "thongBaoLaTeacher");
    existingParticipants4TMsg.add("data", participantsArray);
    log.info("TEACHER {}: sending a list of {} students", teacher.getName(),
        participantsArray.size());
    teacher.sendMessage(existingParticipants4TMsg);
  }


  public void sendTeacherName(UserSession user) throws IOException {
    final JsonObject infoteacherMsg = new JsonObject();
    infoteacherMsg.addProperty("id", "teacherInfo");
    infoteacherMsg.addProperty("teacher", teacher.getName());
    log.debug("PARTICIPANT {}: {} la` teacher",user.getName(),teacher.getName());
    user.sendMessage(infoteacherMsg);
  }
  public void sendParticipantNames(UserSession user) throws IOException {

    final JsonArray participantsArray = new JsonArray();
    //Gui danh sach participant cho new comer (tru` teacher)
    for (final UserSession participant : this.getParticipants()) {
      if (participant != user) {
        final JsonElement participantName = new JsonPrimitive(participant.getName());
        participantsArray.add(participantName);
      }
    }

    final JsonObject existingParticipantsMsg = new JsonObject();
    existingParticipantsMsg.addProperty("id", "existingParticipants");
    existingParticipantsMsg.add("data", participantsArray);
    log.debug("PARTICIPANT {}: sending a list of {} participants", user.getName(),
        participantsArray.size());
    user.sendMessage(existingParticipantsMsg);
  }

  public Collection<UserSession> getParticipants() {
    return participants.values();
  }

  public UserSession getParticipant(String name) {
    return participants.get(name);
  }
  public UserSession getTeacher() {
    return this.teacher;
  }
  public void standUp(String speakerName) throws IOException {
    this.speaker = getParticipant(speakerName);
    final JsonObject newSpeakerMsg = new JsonObject();
    newSpeakerMsg.addProperty("id", "newSpeaker");
    newSpeakerMsg.addProperty("name", speaker.getName());

    log.info("Send message New Speaker: {} to all members", speaker.getName());
    for (final UserSession participant : participants.values()) {
      try {
        participant.sendMessage(newSpeakerMsg);
      } catch (final IOException e) {
        log.debug("ROOM {}: participant {} could not be notified", name, participant.getName(), e);
      }
    }
    if (teacher != null){
      teacher.sendMessage(newSpeakerMsg);
    }
  }
  public void broadcastMsg(JsonObject msg) throws IOException {
    for (final UserSession participant : this.getParticipants()) {
        participant.sendMessage(msg);
      }
      if(teacher!=null) {teacher.sendMessage(msg);}
  }

  @Override
  public void close() {
    for (final UserSession user : participants.values()) {
      try {
        user.close();
      } catch (IOException e) {
        log.debug("ROOM {}: Could not invoke close on participant {}", this.name, user.getName(),
            e);
      }
    }

    participants.clear();

    pipeline.release(new Continuation<Void>() {

      @Override
      public void onSuccess(Void result) throws Exception {
        log.trace("ROOM {}: Released Pipeline", Room.this.name);
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        log.warn("PARTICIPANT {}: Could not release Pipeline", Room.this.name);
      }
    });

    log.debug("Room {} closed", this.name);
  }

}
