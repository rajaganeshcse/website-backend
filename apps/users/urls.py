from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView
from .views import LoginView, VerifyView

urlpatterns = [
    path('login/',   LoginView.as_view()),
    path('verify/',  VerifyView.as_view()),
    path('refresh/', TokenRefreshView.as_view()),
]
