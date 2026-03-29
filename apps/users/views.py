from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth.models import User


class LoginView(APIView):
    def post(self, request):
        username = request.data.get('username', '').strip()
        password = request.data.get('password', '').strip()

        try:
            user = User.objects.get(username=username)
        except User.DoesNotExist:
            return Response({'error': 'User not found'}, status=401)

        if not user.check_password(password):
            return Response({'error': 'Wrong password'}, status=401)

        if not user.is_active:
            return Response({'error': 'Account inactive'}, status=401)

        refresh = RefreshToken.for_user(user)
        return Response({
            'access':   str(refresh.access_token),
            'refresh':  str(refresh),
            'username': user.username,
        })


class VerifyView(APIView):
    def get(self, request):
        from rest_framework_simplejwt.authentication import JWTAuthentication
        auth = JWTAuthentication()
        try:
            user, _ = auth.authenticate(request)
            return Response({'valid': True, 'username': user.username})
        except:
            return Response({'valid': False}, status=401)