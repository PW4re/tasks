import re
import sys
import socket

from sys import platform
from typing import List
from subprocess import Popen, PIPE


class Parser:
    @classmethod
    def parse_output(cls, target: str):
        if platform == 'linux' or platform == 'linux2' or platform == 'darwin':
            cls._parse_traceroute_output(target)
        elif platform == 'win32':
            return cls._parse_tracert_output(target)

    @staticmethod
    def _parse_traceroute_output(target: str) -> List[str]:
        pass

    @staticmethod
    def _parse_tracert_output(target: str) -> List[str]:
        split_regex = re.compile(b'\s+')
        ip_regex = re.compile(b'^\d{1,3}\\.\d{1,3}\\.\d{1,3}\\.\d{1,3}$')
        with Popen(f'tracert -d {target}', shell=True, stdout=PIPE) as proc:
            result = []
            for line in proc.stdout.readlines():
                for res in re.split(split_regex, line):
                    ip = re.search(ip_regex, res)
                    if ip:
                        result.append(ip.string.decode())

        return result


class WhoisParser:
    NREFER = 'nrefer:'

    def __init__(self):
        pass

    @classmethod
    def parse_IANA_answer(cls, answer: str) -> str:
        for line in answer.split('\n'):
            if line.startswith(cls.NREFER):
                return line.replace(cls.NREFER, '').replace(' ', '').strip()


class WhoisQuestioner:
    def __init__(self, addresses):
        self.addresses = addresses
        self.port = 43
        self.host_IANA = socket.gethostbyname('whois.iana.org.')

    def ask(self):
        ip_with_registrator = {}
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as m_socket:
            m_socket.connect((self.host_IANA, self.port))
            for ip in self.addresses:
                m_socket.sendall(b'\n\r')
            data = m_socket.recv(1024)
            print(repr(data))


if __name__ == '__main__':
    WhoisQuestioner(['as']).ask()
    # questioner = WhoisQuestioner(Parser.parse_output('ya.ru'))
    # questioner.ask()
