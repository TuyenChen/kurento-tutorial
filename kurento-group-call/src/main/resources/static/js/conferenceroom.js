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

var ws = new WebSocket('wss://' + location.host + '/groupcall');
var participants = {};
var students = {};
var rooms = {};
var room;
var name;
var teacher;

window.onbeforeunload = function() {
	ws.close();
};

ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
	case 'chat':
		$('#messages').append($('<li>').text(parsedMessage.name+': '+ parsedMessage.content));
		break;
	case 'loginFail':
		console.log('login fail');
		location.reload();
		break;
	case 'loginSuccess':
		console.log('Login Success \n Array rooms:'+parsedMessage.data);
		viewHall(parsedMessage.data); 
		break;
	case 'thongBaoLaTeacher':
		hanhDongNhuTeacher(parsedMessage);
		break;
	case 'teacherInfo':
		teacherComeIn(parsedMessage);
		break;
	case 'existingParticipants':
		onExistingParticipants(parsedMessage);
		break;
	case 'newParticipantArrived':
		onNewParticipant(parsedMessage);
		break;
	case 'participantLeft':
		onParticipantLeft(parsedMessage);
		break;
	case 'teacherLeft':
		onTeacherLeft(parsedMessage);
		break;
	case 'receiveVideoAnswer':
		receiveVideoResponse(parsedMessage);
		break;
	case 'iceCandidate':
		participants[parsedMessage.name].rtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
	        if (error) {
		      console.error("Error adding candidate: " + error);
		      return;
	        }
	    });
	    break;
	default:
		console.error('Unrecognized message', parsedMessage);
	}
}
function login() {
	// ws = new WebSocket('wss://' + location.host + '/groupcall');
	name = document.getElementById('name').value;
	var password = document.getElementById('password').value;
	var message = {
		id : 'login',
		name : name,
		password : password,
	}
	sendMessage(message);
}

function viewHall(arrayRooms) {
	document.getElementById('join').style.display = 'none';
	document.getElementById('hall').style.display = 'block';
	arrayRooms.forEach(newRoom);
}
function newRoom (nameRoom) {
	var nroom = new Room(nameRoom);
	rooms[nameRoom] = nroom;
}

function register() {
	name = document.getElementById('name').value;
	var room = document.getElementById('roomName').value;

	document.getElementById('room-header').innerText = 'ROOM ' + room;
	document.getElementById('join').style.display = 'none';
	document.getElementById('room').style.display = 'block';

	var message = {
		id : 'joinRoom',
		name : name,
		room : room,
	}
	sendMessage(message);
}

function onNewParticipant(request) {
	console.log("******* Newbie"+request.name+" *******");
	var participant = new Student(request.name);
	participants[request.name] = participant;
}

function receiveVideoResponse(result) {
	console.log('$$$$$$'+result.name+'$$$$$$');
	participants[result.name].rtcPeer.processAnswer (result.sdpAnswer, function (error) {
		if (error) return console.error (error);
	});
}

function callResponse(message) {
	if (message.response != 'accepted') {
		console.info('Call not accepted by peer. Closing call');
		stop();
	} else {
		webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
			if (error) return console.error (error);
		});
	}
}
function chooseRoom(nameRoom) {
	document.getElementById('room-header').innerText = 'ROOM ' + nameRoom;
	document.getElementById('hall').style.display = 'none';
	document.getElementById('room').style.display = 'block';
	var message = {
		id : 'joinRoom',
		name : name,
		room : nameRoom,
	}
	room = nameRoom;
	console.log("Send msg choose Room: "+nameRoom+". Name: "+name);
	sendMessage(message);
}
function hanhDongNhuTeacher(msg) {

	var constraints = {
		audio : true,
		video : {
			mandatory : {
				maxWidth : 320,
				maxFrameRate : 15,
				minFrameRate : 15
			}
		}
	};
	console.log(name + " registered in room " + room);
	teacher = new Teacher(name);
	participants[name] = teacher;
	var video = teacher.getVideoElement();
	var options = {
	      localVideo: video,
	      mediaConstraints: constraints,
	      onicecandidate: teacher.onIceCandidate.bind(teacher)
	    }
	teacher.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
		function (error) {
		  if(error) {
			  return console.error(error);
		  }
		  this.generateOffer (teacher.offerToReceiveVideo.bind(teacher));
	});
	
	msg.data.forEach(newStudent);

} 
function teacherComeIn(msg) {
	console.log("TEACHERRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR: "+msg.teacher);
	teacher = new Teacher(msg.teacher);
	participants[msg.teacher] = teacher;
	var video = teacher.getVideoElement();

	var options = {
      remoteVideo: video,
      onicecandidate: teacher.onIceCandidate.bind(teacher)
    }
    console.log("Number of PARTICIPANTS:\n"+participants.length);
	
	teacher.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
			function (error) {
			  if(error) {
				  return console.error(error);
			  }
			  this.generateOffer (teacher.offerToReceiveVideo.bind(teacher));
	});;
	
}

function newNoobJoin(name) {
	console.log(name + " registered in room " + room);
	var participant = new Student(name);
	participants[name] = participant;
}
function newStudent(name) {
	console.log("Student: "+name + " come in room " + room);
	var student = new Student(name);
	students[name] = student;
	participants[name] = student;
}
function onExistingParticipants(msg) {
	// receiveVideo(msg.Teacher);
	// var constraints = {
	// 	audio : true,
	// 	video : {
	// 		mandatory : {
	// 			maxWidth : 320,
	// 			maxFrameRate : 15,
	// 			minFrameRate : 15
	// 		}
	// 	}
	// };
	// console.log(name + " registered in room " + room);
	// var participant = new Participant(name);
	// participants[name] = participant;
	// var video = participant.getVideoElement();

	// var options = {
	//       localVideo: video,
	//       mediaConstraints: constraints,
	//       onicecandidate: participant.onIceCandidate.bind(participant)
	//     }
	// participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
	// 	function (error) {
	// 	  if(error) {
	// 		  return console.error(error);
	// 	  }
	// 	  this.generateOffer (participant.offerToReceiveVideo.bind(participant));
	// });

	msg.data.forEach(newNoobJoin);
}

function leaveRoom() {
	sendMessage({
		id : 'leaveRoom'
	});

	for ( var key in participants) {
		participants[key].dispose();
	}

	document.getElementById('join').style.display = 'block';
	document.getElementById('room').style.display = 'none';
	document.getElementById('hall').innerHTML = '';
	// ws.close();
}

function receiveVideo(sender) {
	var participant = new Participant(sender);
	participants[sender] = participant;
	var video = participant.getVideoElement();

	var options = {
      remoteVideo: video,
      onicecandidate: participant.onIceCandidate.bind(participant)
    }

	participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
			function (error) {
			  if(error) {
				  return console.error(error);
			  }
			  this.generateOffer (participant.offerToReceiveVideo.bind(participant));
	});;
}

function onParticipantLeft(request) {
	console.log('Participant ' + request.name + ' left');
	var participant = participants[request.name];
	participant.dispose();
	delete participants[request.name];
}
function onTeacherLeft(msg) {
	console.log('Teacher left');
	teacher.dispose();
	teacher = null;
}
function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	// console.log('Senging message: ' + jsonMessage);
	ws.send(jsonMessage);
}
