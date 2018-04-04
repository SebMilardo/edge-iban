#!/usr/bin/env python2
# -*- coding: utf-8 -*-

import socket
import time

UDP_IP = "127.0.0.1"
UDP_PORT = 4445


print "UDP target IP:", UDP_IP
print "UDP target port:", UDP_PORT


sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(("0.0.0.0", 4443))

timestamp = str(time.time())

sock.sendto(timestamp, (UDP_IP, UDP_PORT))

data, addr = sock.recvfrom(1024) # buffer size is 1024 bytes

print "Sent: ", timestamp
print "Received: ", data