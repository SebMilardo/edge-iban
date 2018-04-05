# sudo sdptool add --channel=22 SP
# hcitool dev

from bluetooth import *

BUFFER_SIZE = 20
UDP_IP = "127.0.0.1"
UDP_PORT_RX = 5002
UDP_PORT_TX = 5004

server_sock = BluetoothSocket(RFCOMM)
server_sock.bind(("", 22))
server_sock.listen(1)

port = server_sock.getsockname()[1]
uuid = "00001101-0000-1000-8000-00805f9b34fb"

advertise_service(server_sock, "btServer",
                  service_id=uuid,
                  service_classes=[uuid, SERIAL_PORT_CLASS],
                  profiles=[SERIAL_PORT_PROFILE],
                  )

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT_RX))

print "UDP-BT Server:"
print "Waiting for UDP packets on port %d" % UDP_PORT_RX
print "Waiting for connection on RFCOMM channel %d" % port

while True:
    delays = list()
    client_sock, client_info = server_sock.accept()
    print "Accepted connection from ", client_info
    try:
        while True:
            data, addr = sock.recvfrom(BUFFER_SIZE)
            client_sock.send(data)
            data = client_sock.recv(BUFFER_SIZE)
            sock.sendto(data, (UDP_IP, UDP_PORT_TX))
    except:
        pass
