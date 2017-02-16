// This is a manifest file that'll be compiled into application.js.
//
// Any JavaScript file within this directory can be referenced here using a relative path.
//
// You're free to add application-wide JavaScript to this file, but it's generally better
// to create separate JavaScript files as needed.
//
//= require sockjs-0.3
//= require stomp
//= require self

if (typeof jQuery !== 'undefined') {
    (function($) {
      var feed_id = $("#feed-watcher").data( "feedid" );
      console.log("data-feedid on body tag %o",feed_id);
      console.log("data on body tag %o",$("#feed-watcher").data());
    })(jQuery);
}


function initEvents() {
  var ws = new SockJS('http://' + window.location.hostname + ':15674/stomp');
  var client = Stomp.over(ws);
  // SockJS does not support heart-beat: disable heart-beats
  client.heartbeat.incoming = 0;
  client.heartbeat.outgoing = 0;
  
  client.debug = function(e) {
    $('#second div').append($("<code>").text(e));
  };
  
  // default receive callback to get message from temporary queues
  client.onreceive = function(m) {
    $('#first div').append($("<code>").text(m.body));
  }
  
  var on_connect = function(x) {
            id = client.subscribe("/queue/test", function(m) {
              // reply by sending the reversed text to the temp queue defined in the "reply-to" header
              var reversedText = m.body.split("").reverse().join("");
              client.send(m.headers['reply-to'], {"content-type":"text/plain"}, reversedText);
            });
  };
  var on_error =  function() {
          console.log('error');
  };
  client.connect('guest', 'guest', on_connect, on_error, '/');
  
  $('#first form').submit(function() {
    var text = $('#first form input').val();
    if (text) {
      client.send('/queue/test', {'reply-to': '/temp-queue/foo'}, text);
      $('#first form input').val("");
    }
    return false;
  });
}

