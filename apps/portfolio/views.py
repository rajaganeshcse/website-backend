import json

from django.db import transaction
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.parsers import MultiPartParser, FormParser, JSONParser
from .models import Hero, Skill, Project, Education, EducationDocument, Internship, Certification, Workshop, ContactMessage
from .serializers import (
    HeroSerializer, SkillSerializer, ProjectSerializer, EducationSerializer,
    InternshipSerializer, CertificationSerializer, WorkshopSerializer, ContactMessageSerializer
)


def number_or(value, fallback=0):
    try:
        return int(value)
    except (TypeError, ValueError):
        return fallback


def pick_education_fields(data, current=None):
    return {
        'degree': data.get('degree', current.degree if current else ''),
        'institution': data.get('institution', current.institution if current else ''),
        'location': data.get('location', current.location if current else ''),
        'year': data.get('year', current.year if current else ''),
        'score': data.get('score', current.score if current else ''),
        'order': number_or(data.get('order'), current.order if current else 0),
    }


def parse_documents_json(raw_value):
    if raw_value in (None, ''):
        return None

    if isinstance(raw_value, list):
        return raw_value

    try:
        return json.loads(raw_value)
    except (TypeError, ValueError, json.JSONDecodeError):
        return []


def build_document_payloads(request, current_documents=None):
    current_documents = list(current_documents or [])
    current_by_id = {str(document.id): document for document in current_documents}
    parsed_documents = parse_documents_json(request.data.get('documents_json'))

    if isinstance(parsed_documents, list):
        payloads = []
        for index, document in enumerate(parsed_documents):
            if not isinstance(document, dict):
                continue

            document_id = str(document.get('id') or '').strip()
            existing_document = current_by_id.get(document_id)
            file_field = str(document.get('fileField') or '').strip()
            uploaded_file = request.FILES.get(file_field) if file_field else None
            title = (
                str(document.get('title') or '').strip()
                or (existing_document.title if existing_document else '')
                or (uploaded_file.name if uploaded_file else '')
                or f'Document {index + 1}'
            )

            if uploaded_file or existing_document:
                payloads.append({
                    'existing': existing_document,
                    'file': uploaded_file,
                    'title': title,
                    'order': index,
                })

        return payloads

    legacy_pdf = request.FILES.get('result_pdf')
    if legacy_pdf:
        return [{
            'existing': None,
            'file': legacy_pdf,
            'title': 'Uploaded PDF',
            'order': 0,
        }]

    return [{
        'existing': document,
        'file': None,
        'title': document.title or document.pdf_name or f'Document {index + 1}',
        'order': index,
    } for index, document in enumerate(current_documents)]


def sync_education_documents(education, documents):
    keep_ids = []

    for payload in documents:
        current_document = payload.get('existing')
        uploaded_file = payload.get('file')

        if current_document is None and uploaded_file is None:
            continue

        if current_document is None:
            current_document = EducationDocument(education=education)

        current_document.education = education
        current_document.title = payload.get('title', '')
        current_document.order = payload.get('order', 0)

        if uploaded_file is not None:
            current_document.pdf = uploaded_file
            current_document.pdf_name = uploaded_file.name or ''

        current_document.save()
        keep_ids.append(current_document.id)

    education.documents.exclude(id__in=keep_ids).delete()


class PublicPortfolioView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        hero = Hero.objects.first()
        return Response({
            'hero':           HeroSerializer(hero, context={'request': request}).data if hero else {},
            'skills':         SkillSerializer(Skill.objects.all(), many=True).data,
            'projects':       ProjectSerializer(Project.objects.all(), many=True, context={'request': request}).data,
            'education':      EducationSerializer(Education.objects.prefetch_related('documents').all(), many=True, context={'request': request}).data,
            'internships':    InternshipSerializer(Internship.objects.all(), many=True, context={'request': request}).data,
            'certifications': CertificationSerializer(Certification.objects.all(), many=True, context={'request': request}).data,
            'workshops':      WorkshopSerializer(Workshop.objects.all(), many=True, context={'request': request}).data,
        })


class PublicContactView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        s = ContactMessageSerializer(data=request.data)
        if s.is_valid():
            s.save()
            return Response({'message': 'Sent!'}, status=201)
        return Response(s.errors, status=400)


class AdminHeroView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def get(self, request):
        hero = Hero.objects.first()
        return Response(HeroSerializer(hero, context={'request': request}).data if hero else {})

    def put(self, request):
        hero, _ = Hero.objects.get_or_create(pk=1)
        s = HeroSerializer(hero, data=request.data, partial=True, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data)
        return Response(s.errors, status=400)


class AdminSkillListView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(SkillSerializer(Skill.objects.all(), many=True).data)

    def post(self, request):
        s = SkillSerializer(data=request.data)
        if s.is_valid():
            s.save()
            return Response(s.data, status=201)
        return Response(s.errors, status=400)


class AdminSkillDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def put(self, request, pk):
        skill = Skill.objects.get(pk=pk)
        s = SkillSerializer(skill, data=request.data, partial=True)
        if s.is_valid():
            s.save()
            return Response(s.data)
        return Response(s.errors, status=400)

    def delete(self, request, pk):
        Skill.objects.get(pk=pk).delete()
        return Response(status=204)


class AdminProjectListView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def get(self, request):
        return Response(ProjectSerializer(Project.objects.all(), many=True, context={'request': request}).data)

    def post(self, request):
        s = ProjectSerializer(data=request.data, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data, status=201)
        return Response(s.errors, status=400)


class AdminProjectDetailView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def put(self, request, pk):
        proj = Project.objects.get(pk=pk)
        s = ProjectSerializer(proj, data=request.data, partial=True, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data)
        return Response(s.errors, status=400)

    def delete(self, request, pk):
        Project.objects.get(pk=pk).delete()
        return Response(status=204)


class AdminEducationListView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def get(self, request):
        queryset = Education.objects.prefetch_related('documents').all()
        return Response(EducationSerializer(queryset, many=True, context={'request': request}).data)

    @transaction.atomic
    def post(self, request):
        education = Education.objects.create(**pick_education_fields(request.data))
        sync_education_documents(education, build_document_payloads(request))
        return Response(EducationSerializer(education, context={'request': request}).data, status=201)


class AdminEducationDetailView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    @transaction.atomic
    def put(self, request, pk):
        education = Education.objects.prefetch_related('documents').get(pk=pk)
        for field, value in pick_education_fields(request.data, current=education).items():
            setattr(education, field, value)
        education.save()
        sync_education_documents(education, build_document_payloads(request, education.documents.all()))
        education.refresh_from_db()
        return Response(EducationSerializer(education, context={'request': request}).data)

    @transaction.atomic
    def delete(self, request, pk):
        Education.objects.get(pk=pk).delete()
        return Response(status=204)


class AdminInternshipListView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def get(self, request):
        return Response(InternshipSerializer(Internship.objects.all(), many=True, context={'request': request}).data)

    def post(self, request):
        s = InternshipSerializer(data=request.data, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data, status=201)
        return Response(s.errors, status=400)


class AdminInternshipDetailView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def put(self, request, pk):
        obj = Internship.objects.get(pk=pk)
        s = InternshipSerializer(obj, data=request.data, partial=True, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data)
        return Response(s.errors, status=400)

    def delete(self, request, pk):
        Internship.objects.get(pk=pk).delete()
        return Response(status=204)


class AdminCertListView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def get(self, request):
        return Response(CertificationSerializer(Certification.objects.all(), many=True, context={'request': request}).data)

    def post(self, request):
        s = CertificationSerializer(data=request.data, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data, status=201)
        return Response(s.errors, status=400)


class AdminCertDetailView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def put(self, request, pk):
        obj = Certification.objects.get(pk=pk)
        s = CertificationSerializer(obj, data=request.data, partial=True, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data)
        return Response(s.errors, status=400)

    def delete(self, request, pk):
        Certification.objects.get(pk=pk).delete()
        return Response(status=204)


class AdminWorkshopListView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def get(self, request):
        return Response(WorkshopSerializer(Workshop.objects.all(), many=True, context={'request': request}).data)

    def post(self, request):
        s = WorkshopSerializer(data=request.data, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data, status=201)
        return Response(s.errors, status=400)


class AdminWorkshopDetailView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def put(self, request, pk):
        obj = Workshop.objects.get(pk=pk)
        s = WorkshopSerializer(obj, data=request.data, partial=True, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data)
        return Response(s.errors, status=400)

    def delete(self, request, pk):
        Workshop.objects.get(pk=pk).delete()
        return Response(status=204)


class AdminMessagesView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(ContactMessageSerializer(ContactMessage.objects.all(), many=True).data)


class PublicGalleryView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        from .models import GalleryImage
        from .serializers import GalleryImageSerializer
        images = GalleryImage.objects.all()
        return Response(GalleryImageSerializer(images, many=True, context={'request': request}).data)


class AdminGalleryListView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def get(self, request):
        from .models import GalleryImage
        from .serializers import GalleryImageSerializer
        images = GalleryImage.objects.all()
        return Response(GalleryImageSerializer(images, many=True, context={'request': request}).data)

    def post(self, request):
        from .models import GalleryImage
        from .serializers import GalleryImageSerializer
        s = GalleryImageSerializer(data=request.data, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data, status=201)
        return Response(s.errors, status=400)


class AdminGalleryDetailView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes     = [MultiPartParser, FormParser, JSONParser]

    def put(self, request, pk):
        from .models import GalleryImage
        from .serializers import GalleryImageSerializer
        obj = GalleryImage.objects.get(pk=pk)
        s = GalleryImageSerializer(obj, data=request.data, partial=True, context={'request': request})
        if s.is_valid():
            s.save()
            return Response(s.data)
        return Response(s.errors, status=400)

    def delete(self, request, pk):
        from .models import GalleryImage
        GalleryImage.objects.get(pk=pk).delete()
        return Response(status=204)
