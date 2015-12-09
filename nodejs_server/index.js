var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);

app.get('/', function(req, res){
  res.sendFile(__dirname + '/index.html');
  console.log('sent homepage');
});

http.listen(7664, function(){
  console.log('listening on *:7664');
});

// **Definitions**

// Logged user: connected user emitted a login message 
// Has socket in sockets object, indexed by it's user_id
var sockets = {};

// Open game: a user game that is waiting for another user to join 
// Has his name in open_games object, indexed by it's user_id
var open_games = {};
// receives 'open game', 'close game'
// emits: 'game opened', 'game closed'

// Running game: a game running between two users
// Has a pair of user_id entries, indexed by the other user's id
var running_games = {};
// receives: 'join game', 'leave game'
// emits: 'game started', 'game stopped'

// Some other rules:
// 1) When an open game turns into a running game, treat as 'close game'
// 2) If an user disconnects with a game open, treat as 'close game'
// 3) If an user disconnects with a running game, treat as 'leave game' 
// ---

io.on('connection', function (socket) {
  console.log('new connection received');
    
  socket.on('login', function(user_id) {
    socket.user_id = user_id;
    sockets[user_id] = socket;
    console.log('login ' + user_id);
    // informs the client that has just logon the list of current open games
    for (var user_id in open_games)
      if (open_games.hasOwnProperty(user_id))
        socket.emit('game opened', { user_id: user_id, user_name: open_games[user_id] });
  });

  function close_game(user_id) {
    if (open_games.hasOwnProperty(user_id)) {
      // close an open game
      socket.broadcast.emit('game closed', { user_id: user_id });
      delete open_games[user_id];
    }
  }
  
  function disconnect_running_game(user_id) {
    if (running_games.hasOwnProperty(user_id)) { // this user_id
      var other_user_id = running_games[user_id]; // the other party user_id
      if (sockets.hasOwnProperty(other_user_id)) {
        sockets[other_user_id].ready = false;
        sockets[other_user_id].emit('game stopped'); // tell other party to leave game
      }
      if (running_games.hasOwnProperty(other_user_id))
        delete running_games[other_user_id]; // delete the other party from running game list
      // suppose that this user_id was the one who requested game stop, so don't need to emit 'game stopped' for it
      // delete this user from running game list
      delete running_games[user_id];
      sockets[user_id].ready = false;
    }
  }

  // when the other client emits a 'leave game'
  socket.on('leave game', function() {
    console.log('leave game ' + socket.user_id);
    disconnect_running_game(socket.user_id);
  });
  
  // when someone disconnects
  socket.on('disconnect', function() {
    if (socket.hasOwnProperty('user_id')) { // if user has logged
      console.log('disconnect ' + socket.user_id);
      disconnect_running_game(socket.user_id);
      close_game(socket.user_id);
      delete sockets[socket.user_id];
      //socket.off(); // does not exist. Do I need to unregister all listeners on server?
    }
  });

  // when the client emits 'open game'
  socket.on('open game', function (user_name) {
    console.log('opening new game ' + socket.user_id + ': ' + user_name);
    // store the game in the global list
    open_games[socket.user_id] = user_name;
    // we tell the client to execute 'new game'
    socket.broadcast.emit('game opened', { user_id: socket.user_id, user_name: user_name });
  });

  // when the client emits 'close game'
  socket.on('close game', function () {
    console.log('close game ' + socket.user_id);
    close_game(socket.user_id);
  });
  
  // when the client emits a 'join game'
  socket.on('join game', function (other_user_id) {
    console.log('joing game from ' + socket.user_id + ' to ' + other_user_id);
	  if (open_games.hasOwnProperty(other_user_id)) {
      var other_user_socket = sockets[other_user_id];
		  // close the game for other users
		  close_game(other_user_id);
		  // add a new running game pair and emit 'start game' for both users
		  running_games[socket.user_id] = other_user_id;
		  running_games[other_user_id] = socket.user_id;
		  other_user_socket.emit('game started', { user_id: socket.user_id });
		  socket.emit('game started', { user_id: other_user_id });
      // now wait for both to send 'ready'
    }
  });
  
  // when the client emits a 'ready'
  socket.on('ready', function() {
    if (running_games.hasOwnProperty(socket.user_id)) {
      var other_user_socket = sockets[running_games[socket.user_id]];
      socket.ready = true;
      if (other_user_socket.ready) {
        // trigger game start when both players are ready
        // Player 1 get the shadow
        socket.emit('ball update', { x_p: 50 });
        // Player 2 get the ball
        var xv = Math.round(Math.random() * 8 - 4); // -4 to +4 (% per interval)
        var yv = Math.round(Math.random() * 4 + 1); // +1 to +5 (% per interval)
        other_user_socket.emit('give control', { x_p: 50, x_v: xv, y_v: yv });
      }
    }
  });
  
  // GAME LOGIC COMMUNICATION
  
  socket.on('ball update', function (xp) {
    if (running_games.hasOwnProperty(socket.user_id)) {
      var other_socket = sockets[running_games[socket.user_id]];
      other_socket.emit('ball update', { x_p: xp });
    }
  });
  
  socket.on('give control', function (xp,xv,yv) {
    if (running_games.hasOwnProperty(socket.user_id)) {
      var other_socket = sockets[running_games[socket.user_id]];
      other_socket.emit('give control', { x_p: xp, x_v: xv, y_v: yv });
    }
  });
  
  socket.on('ball out', function() {
    if (running_games.hasOwnProperty(socket.user_id)) {
      var other_socket = sockets[running_games[socket.user_id]];
      other_socket.emit('score');
      // random new start
      var xv = Math.round(Math.random() * 8 - 4); // -4 to +4 (% per interval)
      var yv = Math.round(Math.random() * 4 + 1); // +1 to +5 (% per interval)
      other_socket.emit('give control', { x_p: 50, x_v: xv, y_v: yv });
    }
  });
  
});
