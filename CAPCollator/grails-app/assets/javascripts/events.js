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
      console.log("feedid on div %o",feed_id);
      console.log("data on body tag %o",$("#feed-watcher").data());
      initEvents(feed_id);
    })(jQuery);
}


function initEvents(feed_id) {

  console.log("Connect to http://"+ window.location.hostname + "/rabbitws/stomp");

  // var ws = new SockJS('http://' + window.location.hostname + '/rabbitws/stomp');
  // var ws = new SockJS('http://' + window.location.hostname + ':15674/stomp');
  var ws = new SockJS('http://' + window.location.hostname + '/rabbitws/stomp');
  
  var client = Stomp.over(ws);

  // SockJS does not support heart-beat: disable heart-beats
  client.heartbeat.incoming = 0;
  client.heartbeat.outgoing = 0;
  
  client.debug = function(e) {
    console.log("debug %o",e);
    // $('#second div').append($("<code>").text(e));
  };
  
  // default receive callback to get message from temporary queues
  client.onreceive = function(m) {
    console.log("message %o",m);
    // $('#first div').append($("<code>").text(m.body));
  }
  
  var on_connect = function(x) {
    console.log("Connected - subscribe to /exchange/CAPExchange/CAPSubMatch.%s",feed_id);

    // id = client.subscribe("/exchange/CAPExchange/CAPSubMatch.#", function(m) {
    var id = client.subscribe("/exchange/CAPExchange/CAPSubMatch."+feed_id, function(m) {
      console.log("/exchange/CAPExchange/CAPSubMatch.%s Got message %o",feed_id,m);
      // reply by sending the reversed text to the temp queue defined in the "reply-to" header
      // var reversedText = m.body.split("").reverse().join("");
      // client.send(m.headers['reply-to'], {"content-type":"text/plain"}, reversedText);
    });

    // var id2 = client.subscribe("/exchange/CAPExchange/CAPSubMatch.#", function(m) {
    //   console.log("/exchange/CAPExchange/CAPSubMatch.# Got message %o",m);
      // reply by sending the reversed text to the temp queue defined in the "reply-to" header
      // var reversedText = m.body.split("").reverse().join("");
      // client.send(m.headers['reply-to'], {"content-type":"text/plain"}, reversedText);
    // });
  };

  var on_error =  function() {
          console.log('error');
  };

  console.log("Connect...");
  client.connect('cap', 'cap', on_connect, on_error, '/');
  console.log("Connect complete...");
}

