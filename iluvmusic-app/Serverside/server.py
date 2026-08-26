from main import app
from auth_routes import auth_api
from profile_routes import profile_api

# Firebase authentication and profile customization routes.
app.register_blueprint(auth_api)
app.register_blueprint(profile_api)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
