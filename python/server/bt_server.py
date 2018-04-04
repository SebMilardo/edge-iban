# sudo sdptool add --channel=22 SP
# hcitool dev

import time
import pandas as pd
from bluetooth import *

server_sock = BluetoothSocket(RFCOMM)
server_sock.bind(("", 22))
server_sock.listen(1)

port = server_sock.getsockname()[1]
uuid = "00001101-0000-1000-8000-00805f9b34fb"

advertise_service(server_sock, "btServer",
                  service_id=uuid,
                  service_classes=[uuid, SERIAL_PORT_CLASS],
                  profiles=[SERIAL_PORT_PROFILE],
                  #                   protocols = [ OBEX_UUID ]
                  )

print "Waiting for connection on RFCOMM channel %d" % port

while True:
    delays = list()
    client_sock, client_info = server_sock.accept()
    print "Accepted connection from ", client_info
    i = 0;
    try:
        while i<1000:
            i=i+1;
	    client_sock.send(str(time.time()))
            data = client_sock.recv(1024)
            if len(data) == 0: break
            delays.append(time.time() - float(data))
        print "Done"
        pd.DataFrame(delays).to_csv("delays_%d.csv" % time.time())
    except IOError:
	pass
