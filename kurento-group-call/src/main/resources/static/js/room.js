function Room(name) {
	this.room = name;
	console.log('room: '+name);
	var container = document.createElement('div');
	container.id = name;
	container.className = 'classroom';
	var span = document.createElement('span');
	container.appendChild(span);
	container.onclick = function(){
		chooseRoom(container.id);
	}
	document.getElementById('hall').appendChild(container);

	span.appendChild(document.createTextNode(name));
}

