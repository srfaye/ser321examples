A simple Echo server that has been multi-threaded to handle multiple clients. The server sends back what it receives.

Run the server:
  - gradle runServer -Pport=PORT

Run the client:
  - gradle runClient -Phost=HOST -Pport=PORT


Has default arguments of host='localhost' and port='8050'

The client takes console input.
