import re
import socket

from dataclasses import dataclass
from subprocess import Popen, PIPE
from sys import platform
from typing import List, Tuple
from multiprocessing.pool import ThreadPool
from itertools import zip_longest


class UtilityParser:
    @classmethod
    def parse_output(cls, target: str):
        if platform == 'linux' or platform == 'linux2' or platform == 'darwin':
            cls._parse_traceroute_output(target)
        elif platform == 'win32':
            return cls._parse_tracert_output(target)

    @staticmethod
    def _parse_traceroute_output(target: str) -> List[bytes]:
        pass

    @staticmethod
    def _parse_tracert_output(target: str) -> List[bytes]:
        split_regex = re.compile(b'\s+')
        ip_regex = re.compile(b'^\d{1,3}\\.\d{1,3}\\.\d{1,3}\\.\d{1,3}$')
        with Popen(f'tracert -d {target}', shell=True, stdout=PIPE) as proc:
            result = []
            for line in proc.stdout.readlines():
                for res in re.split(split_regex, line):
                    ip = re.search(ip_regex, res)
                    if ip:
                        result.append(ip.string)

        return result


class WhoisParser:
    _REFER = 'refer:'
    _COUNTRY = b'country:'
    _ORIGIN = b'origin:'
    _ROLE = b'role:'

    @classmethod
    def parse_IANA_answer(cls, answer: str) -> str:
        for line in answer.split('\n'):
            if line.startswith(cls._REFER):
                return line.replace(cls._REFER, '').strip()

    @classmethod
    def parse_regional_registrar_answer(
            cls, answer: bytes) -> Tuple[str, str, str]:
        country, origin, role = '', '', ''
        for line in answer.split(b'\n'):
            if line.startswith(cls._COUNTRY):
                country = line.replace(cls._COUNTRY, b'').strip()
            elif line.startswith(cls._ORIGIN):
                origin = line.replace(cls._ORIGIN, b'').strip()
            elif line.startswith(cls._ROLE):
                role = line.replace(cls._ROLE, b'').strip()

        encoding = 'utf-8'
        return (country.decode(encoding),
                origin.decode(encoding),
                role.decode(encoding))


class WhoisQuestioner:
    GLOBAL_REGISTRAR = 'whois.iana.org.'

    @dataclass
    class _RequiredInfo:
        def __init__(self, index: int,
                     number_of_AS: str, country: str, provider: str):
            self.index: int = index
            self.AS_number: str = number_of_AS
            self.country: str = country
            self.provider: str = provider

        def __str__(self):
            return f'{self.index}, {self.AS_number}, {self.country}, {self.provider}'

    def __init__(self, addresses):
        self.addresses: List[bytes] = addresses
        self.port: int = 43
        self.host_IANA: str = socket.gethostbyname(self.GLOBAL_REGISTRAR)
        self.ip_with_info = {}

    def build_table(self):
        with ThreadPool() as pool:
            results = [
                pool.apply_async(
                    self._ask_regional_registrar, args=(self._ask_IANA(ip), ip)
                ) for ip in self.addresses
            ]

            for i in range(len(results)):
                ip = self.addresses[i]
                self.ip_with_info[ip] = self._RequiredInfo(
                    i + 1, *results[i].get()
                )

        self._print_table()

    def _print_table(self):
        lines = []
        columns_len = {
            "Порядковый номер": 0, "IP-адрес": 0, "AS": 0,
            "Страна": 0, "Провайдер": 0
        }

        for ip in self.ip_with_info:
            info = self.ip_with_info[ip]
            lines.append(
                (info.index, ip, info.AS_number, info.country, info.provider)
            )

        # Что-то сложно...


    def _ask_IANA(self, ip) -> str:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as m_socket:
            m_socket.connect((self.host_IANA, self.port))
            m_socket.sendall(b'%b\r\n' % ip)
            data = m_socket.recv(65536)

            return WhoisParser.parse_IANA_answer(data.decode(encoding='utf-8'))

    def _ask_regional_registrar(self, registrar, ip) -> Tuple[str, str, str]:
        if not registrar:
            return '', '', ''
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.connect((socket.gethostbyname(registrar), self.port))
            sock.sendall(b'%b\r\n' % ip)
            full_answer = b''
            while True:
                data = sock.recv(65536)
                if not data:
                    break
                full_answer += data

        return WhoisParser.parse_regional_registrar_answer(full_answer)


if __name__ == '__main__':
    # WhoisQuestioner(['as']).ask()
    questioner = WhoisQuestioner(UtilityParser.parse_output('ya.ru'))
    questioner.build_table()
