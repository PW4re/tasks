import socket
import struct
import datetime

from multiprocessing.pool import ThreadPool

EPOCH_TIME = datetime.datetime(1900, 1, 1)
HOST = '127.0.0.2'
PORT = 123
thread_pool = ThreadPool(10)


def create_server() -> socket.socket:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((HOST, PORT))
    return sock


def get_fake_time(delta: int) -> bytes:
    return get_time_bytes(
        (datetime.datetime.utcnow()-EPOCH_TIME).total_seconds()+delta
    )


def get_time_bytes(_time):
    sec, mil_sec = [int(x) for x in str(_time).split('.')]
    return struct.pack('!II', sec, mil_sec)


def get_sntp_packet(input_packet: bytes, recv_time: bytes, delta: int) -> bytes:
    first_byte = struct.pack('!B', (0 << 6 | 3 << 3 | 4))
    stratum = struct.pack('!B', 1)
    poll = struct.pack('!b', 7)  # 2**7 seconds
    precision = struct.pack('!b', -20)
    delay = struct.pack('!i', 0)
    dispersion = struct.pack('!i', 0)
    ref_id = struct.pack('!i', 0)
    fake_begin_time = get_fake_time(delta)
    input_time = input_packet[40:48]
    return (first_byte + stratum + poll + precision + delay +
           dispersion + ref_id + fake_begin_time + input_time + recv_time)


def start_answer(input_packet: bytes, delta: int, sock: socket.socket, addr: str):
    recv_time = get_fake_time(delta)
    answer = get_sntp_packet(input_packet, recv_time, delta)
    sock.sendto(answer + get_fake_time(delta), addr)


def main(delta: int):
    sock = create_server()
    while True:
        data, addr = sock.recvfrom(1024)
        print(f'{addr} accept')
        thread_pool.apply_async(start_answer, args=(data, delta, sock, addr))


if __name__ == '__main__':
    try:
        try:
            with open("delta.config", 'r') as f:
                try:
                    delta = int(f.readline())
                except ValueError:
                    print('Проверьте формат файла delta.config')
                    exit(4)
        except FileNotFoundError:
            print('Проверьте наличие файла delta.config')
            exit(3)
        main(delta)
    except KeyboardInterrupt:
        exit(0)
