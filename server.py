from socket import socket
def start(port):
    s = socket()
    s.bind(('0.0.0.0', port))
    s.listen(2)
    desktop, d_addr = s.accept()
    print 'Desktop connected at ', d_addr
    android, a_addr = s.accept()
    print 'Android connected at ', a_addr
    while True:
            try:
                    data = android.recv(1024)
                    if data:
                            desktop.send(data)
            except Exception as e:
                    print e
                    break

start(27015)
