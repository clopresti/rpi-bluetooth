import bluetooth
import RPi.GPIO as GPIO

def readlines(socket):
    buffer = socket.recv(1024)
    buffering = True
    while buffering:
        if "\n" in buffer:
            (line, buffer) = buffer.split("\n", 1)
            yield line
        else:
            more = socket.recv(1024)
            if not more:
                buffering = False
            else:
                buffer += more
    if buffer:
        yield buffer

#Setup GPIO        
LED_PIN = 7
GPIO.setmode(GPIO.BOARD)
GPIO.setup(LED_PIN, GPIO.OUT)

server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)

server_sock.bind(("", bluetooth.PORT_ANY))
server_sock.listen(1)
port = server_sock.getsockname()[1]

#Arbitrary uuid - must match Android side
sid = "133f71c6-b7b6-437e-8fd1-d2f59cc76066"

bluetooth.advertise_service(server_sock,
        'RPiServer',
        service_id = sid,
        service_classes = [sid, bluetooth.SERIAL_PORT_CLASS],
        profiles = [bluetooth.SERIAL_PORT_PROFILE]
    )

client_sock, address = server_sock.accept()
print "Connection from ", address

for line in readlines(client_sock):
    print "Received [%s]" % line
    if line == "quit":
        break
    if line == "on":
        GPIO.output(LED_PIN, GPIO.HIGH)
    if line == "off":
        GPIO.output(LED_PIN, GPIO.LOW)

GPIO.cleanup()        

client_sock.close()
server_sock.close()
