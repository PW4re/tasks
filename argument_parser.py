import argparse
import sys

from enum import Enum


class DataTypes(str, Enum):
    FRIENDS = 'friends',
    PHOTOS = 'photos'


class ArgumentParser:
    parser = argparse.ArgumentParser()

    @classmethod
    def parse_arguments(cls):
        cls.parser.add_argument(
            'screen_name',
            type=str,
            help='Короткое имя пользователя, данные которого нужно получить.'
                 'Короткое имя - то же самое, что и VK-id'
        )
        cls.parser.add_argument(
            'data_type',
            type=str,
            help='Данные какого типа нужно получить. '
                 'Возможные типы: friends, photos'
        )
        cls.parser.add_argument(
            'access_token',
            type=str,
            help='актуальный access_token приложения'
        )

        args = cls.parser.parse_args()
        if (args.data_type != DataTypes.FRIENDS.value and
                args.data_type != DataTypes.PHOTOS.value):
            print('Возможные значения второго параметра: friends, photos')
            sys.exit(7)

        return args
