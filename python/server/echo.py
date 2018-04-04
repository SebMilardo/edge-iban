#!/usr/bin/env python2
# -*- coding: utf-8 -*-

import socket

UDP_IP = "0.0.0.0"
UDP_PORT = 4445

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
sock.bind(("0.0.0.0", 4445))

while True:
    data, addr = sock.recvfrom(1024) # buffer size is 1024 bytes
    print "received message:", data
    sock.sendto(data, (UDP_IP, 4443))
