import sys
from typing import List

import socket
import json
import ssl

from argument_parser import DataTypes, ArgumentParser


# access_token 79ae4f1eeebfd5c40855719aa6ac109c2b492c630ccc3e7217b0b475d47dfaae06f8f404c0d10f761f6f3&expires_in=86400
# my vk id 271015050


class OutputCrafter:
    def craft_albums_list(self, response: str):
        try:
            response_parts = json.loads(response)['response']
        except KeyError:
            self._check_error(json.loads(response))
            sys.exit(5)
        print('Названия фотоальбомов: ' + ' '.join(
            [item['title'] for item in response_parts['items']]
        ))

    def craft_friends_table(self, response: str):
        try:
            response_parts = json.loads(response)['response']
        except KeyError:
            self._check_error(json.loads(response))
            sys.exit(5)
        head: List[str] = ['Имя', 'Фамилия', 'Пол', 'Город', 'Онлайн',
                           'Университет', 'Факультет', 'Год окончания']
        lines = self._build_lines_for_friends_table(response_parts)
        lengths = [len(x) for x in head]
        for line in lines:
            lengths[0] = max(lengths[0], len(line[0]))
            lengths[1] = max(lengths[1], len(line[1]))
            lengths[2] = max(lengths[2], len(line[2]))
            lengths[3] = max(lengths[3], len(line[3]))
            lengths[4] = max(lengths[4], len(line[4]))
            lengths[5] = max(lengths[5], len(line[5]))
            lengths[6] = max(lengths[6], len(line[6]))
            lengths[7] = max(lengths[7], len(line[7]))
        self._print_table_line([head], lengths)
        print('-' * (sum(lengths) + len(lengths)))
        self._print_table_line(lines, lengths)

    @staticmethod
    def _check_error(response_parts):
        try:
            print('VK said: ' + response_parts['error']['error_msg'])
        except KeyError:
            print('Ошибка доступа к ресурсу')
            sys.exit(8)

    @staticmethod
    def _print_table_line(lines, lengths):
        for line in lines:
            parts: List[str] = []
            for i in range(len(line)):
                parts.append(
                    line[i].strip('\n').strip('\r').center(lengths[i]))

            print('|'.join(parts))

    def _build_lines_for_friends_table(self, response_parts) -> List[tuple]:
        """get list of 8-elem tuples"""
        lines: List[tuple] = []
        city: str
        graduation: str
        for item in response_parts['items']:
            try:
                try:
                    city = item['city']['title']
                except KeyError:
                    city = 'Не указан'
                graduation = self._try_to_read_key(item, 'graduation')
                lines.append(
                    (item['first_name'], item['last_name'],
                     'Женский' if item['sex'] == 1 else 'Мужской',
                     city, '-' if item['online'] == 0 else '+',
                     self._try_to_read_key(item, 'university_name'),
                     self._try_to_read_key(item, 'faculty_name'),
                     str(graduation))
                )
            except KeyError as e:
                print(e)
                pass

        return lines

    @staticmethod
    def _try_to_read_key(response_part, key):
        try:
            result = response_part[key]
            if not result:
                return 'Не указан'
            return result
        except KeyError:
            return 'Не указан'


class VkAPIAdapter:
    scheme = 'https://'
    api_addr = 'api.vk.com'
    method = '/method'
    https_port = 443

    def __init__(self, screen_name, access_token):
        self.access_token = access_token
        self.user_id = self._resolve_screen_name_to_user_id(screen_name)

    def photo_request(self):
        return self._get_data_from_socket(
            self.method + f'/photos.getAlbums?owner_id={self.user_id}'
                          f'&v=5.131&access_token={self.access_token}'
                          f'&expires_in=86400')

    def friends_request(self):
        return self._get_data_from_socket(
            self.method + f'/friends.get?user_id={self.user_id}&'
            f'fields=sex,city,education,online&v=5.21&'
            f'access_token={self.access_token}&expires_in=86400'
        )

    def _resolve_screen_name_to_user_id(self, screen_name: str):
        response_body = self._get_data_from_socket(
            self.method + f'/utils.resolveScreenName?screen_name={screen_name}&' +
            f'v=5.131&access_token={self.access_token}&expires_in=86400'
        )
        try:
            json_body = json.loads(response_body)
            if not json_body['response']:
                print('Что-то не так с введённым коротким именем')
                sys.exit(4)
            return json_body['response']['object_id']
        except KeyError:
            print('Что-то не так с введённым коротким именем')
            sys.exit(4)

    def _get_data_from_socket(self, http_response_headline):
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.connect((self.api_addr, self.https_port))
                s = ssl.wrap_socket(s, keyfile=None, certfile=None,
                                    server_side=False,
                                    cert_reqs=ssl.CERT_NONE,
                                    ssl_version=ssl.PROTOCOL_TLSv1)
                s.sendall(
                    f'GET {http_response_headline} HTTP/1.1\r\nHost: api.vk.com\r\n'
                    f'Connection: close\r\n\r\n'.encode('cp1251'))

                data = b''
                while True:
                    new = s.recv(4096)
                    if not new:
                        break
                    data += new

            return data.split(b'\r\n\r\n')[-1].decode('utf-8')
        except socket.error:
            print('Проблемы с соединением(')
            sys.exit(9)


if __name__ == '__main__':
    args = ArgumentParser.parse_arguments()
    adapter = VkAPIAdapter(args.screen_name, args.access_token)
    crafter = OutputCrafter()
    if args.data_type == DataTypes.PHOTOS:
        crafter.craft_albums_list(adapter.photo_request())
    else:
        crafter.craft_friends_table(adapter.friends_request())
