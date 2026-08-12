from main import app
from auth_routes import auth_api

# Register Firebase authentication after the existing Flask app is created.
app.register_blueprint(auth_api)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
