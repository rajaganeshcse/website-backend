from django.contrib import admin
from .models import Hero, Skill, Project, Education, EducationDocument, Internship, Certification, Workshop, ContactMessage

admin.site.register(Hero)
admin.site.register(Skill)
admin.site.register(Project)
admin.site.register(Education)
admin.site.register(EducationDocument)
admin.site.register(Internship)
admin.site.register(Certification)
admin.site.register(Workshop)
admin.site.register(ContactMessage)
from .models import GalleryImage
admin.site.register(GalleryImage)
