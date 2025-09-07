from flask import Blueprint, request, jsonify
from . import obst_processor as processor

obstacle_bp = Blueprint('obstacle_bp', __name__)

@obstacle_bp.route('/analyze-obstacle', methods=['POST'])
def analyze_obstacle_route():
    data = request.get_json()
    if not data or 'image' not in data:
        return jsonify({"error": "No image data provided"}), 400

    base64_image = data.get("image")
    level = data.get("level", 2)
    
    try:
        image_data = base64.b64decode(base64_image)
        result = processor.analyze_obstacle_image(image_data, level)
        
        if result.get("success"):
            return jsonify(result)
        else:
            return jsonify({"error": result.get("error")}), 500
            
    except Exception as e:
        return jsonify({"error": f"Invalid image or processing error: {str(e)}"}), 400