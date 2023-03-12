from flask import Flask, json

data = [{"class": "gunbreaker", "name": "Thancred Waters"},
        {"class": "black mage", "name": "Y'shtola Rhul"},
        {"class": "Sage", "name": "Alphinaud Leveilleur"}]


api = Flask(__name__)


@api.route('/data', methods=['GET'])
def get_data():
    return json.dumps(data)


@api.route('/post', methods=['POST'])
def post_data():
    return json.dumps({"success": True}), 201


if __name__ == '__main__':
    api.run(host="0.0.0.0", port=4444, debug=True)
