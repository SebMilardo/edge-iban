import socket
import time

import numpy as np
import pandas as pd
from matplotlib import pyplot as plt

# 0) START -> client.py
# 1) client.py -> udp:/localhost:5001 (GNU-Radio)
# 2) GNU-Radio -> udp:/localhost:5002 (bt_server.py)
# 3) bt_server.py -> Bluetooth RFCOMM 22 (Mobile phone)
# 4) Mobile phone -> tcp:/edge_or_cloud_ip:5003 (Edge/Cloud)
# 5) Edge/Cloud -> Mobile phone
# 6) Mobile phone -> Bluetooth RFCOMM 22 (bt_server.py)
# 7) bt_server.py -> udp:/localhost:5004 (GNU-Radio)
# 8) GNU-Radio -> udp:/localhost:5000 (client.py)
# 9) client.py -> END

UDP_IP = "127.0.0.1"
UDP_PORT_RX = 5000
UDP_PORT_TX = 5002
BUFFER_SIZE = 20

print "UDP target IP:", UDP_IP
print "UDP target port:", UDP_PORT_TX

delays = list()

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.settimeout(1)
sock.bind((UDP_IP, UDP_PORT_RX))
print "Listening on UDP port %d" % UDP_PORT_RX

for i in range(1000):
    try:
        sock.sendto(str(time.time()), (UDP_IP, UDP_PORT_TX))
        
        data, addr = sock.recvfrom(BUFFER_SIZE)
        if len(data) > 0:
            delays.append(time.time() - float(data))
        if i % 100 == 0:
            print "Running... %d %%" % (i/10);
    except:
        print "Timeout..."
        pass
print "Done"
df = pd.DataFrame(delays)
df.to_csv("data/delays_%d.csv" % time.time())
df.plot.hist(bins=np.arange(0, 0.25, 0.005))
plt.savefig("gfx/delays.pdf")
plt.show()
