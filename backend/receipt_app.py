from flask import Flask
from receipt.api.routes import receipt_bp
from obstacle.obst_routes import obstacle_bp

app = Flask(__name__)
# 영수증 분석 API를 /api/receipt 경로에 등록
app.register_blueprint(receipt_bp, url_prefix='/api/receipt')

# 장애물 분석 API를 /api/obstacle 경로에 등록
app.register_blueprint(obstacle_bp, url_prefix='/api/obstacle')


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)