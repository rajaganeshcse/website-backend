from django.db import models


class Hero(models.Model):
    name      = models.CharField(max_length=100, default='Neelamohan R')
    title     = models.CharField(max_length=200, default='Developer & Data Enthusiast')
    bio       = models.TextField(default='')
    email     = models.EmailField(default='neelamohan6cs@gmail.com')
    phone     = models.CharField(max_length=20, default='+91 9500205859')
    github    = models.URLField(default='https://github.com/Neelamohan6cs')
    linkedin  = models.URLField(default='https://www.linkedin.com/in/neelamohan-r-cs')
    portfolio = models.URLField(default='https://neelamohan-profile.vercel.app/')
    photo     = models.ImageField(upload_to='photos/', blank=True, null=True)

    class Meta:
        db_table  = 'hero'
        app_label = 'portfolio'


class Skill(models.Model):
    CATEGORIES = [
        ('programming', 'Programming'),
        ('frontend',    'Frontend'),
        ('backend',     'Backend'),
        ('database',    'Database'),
        ('analytics',   'Data Analytics'),
        ('ml',          'Machine Learning'),
        ('tools',       'Tools'),
    ]
    category = models.CharField(max_length=50, choices=CATEGORIES)
    name     = models.CharField(max_length=100)
    level    = models.IntegerField(default=80)
    order    = models.IntegerField(default=0)

    class Meta:
        db_table  = 'skill'
        app_label = 'portfolio'
        ordering  = ['category', 'order']


class Project(models.Model):
    title       = models.CharField(max_length=200)
    description = models.TextField()
    tech_stack  = models.CharField(max_length=500)
    github_url  = models.URLField(blank=True, default='')
    live_url    = models.URLField(blank=True, default='')
    video_url   = models.URLField(blank=True, default='')
    image       = models.ImageField(upload_to='projects/', blank=True, null=True)
    order       = models.IntegerField(default=0)

    class Meta:
        db_table  = 'project'
        app_label = 'portfolio'
        ordering  = ['order']


class Education(models.Model):
    degree      = models.CharField(max_length=200)
    institution = models.CharField(max_length=300)
    location    = models.CharField(max_length=255, blank=True, default='')
    year        = models.CharField(max_length=20)
    score       = models.CharField(max_length=50)
    order       = models.IntegerField(default=0)

    class Meta:
        db_table  = 'education'
        app_label = 'portfolio'
        ordering  = ['order']


class EducationDocument(models.Model):
    education = models.ForeignKey(Education, related_name='documents', on_delete=models.CASCADE)
    title     = models.CharField(max_length=200, blank=True, default='')
    pdf       = models.FileField(upload_to='education/', blank=True, null=True)
    pdf_name  = models.CharField(max_length=255, blank=True, default='')
    order     = models.IntegerField(default=0)

    class Meta:
        db_table  = 'education_document'
        app_label = 'portfolio'
        ordering  = ['order', 'id']


class Internship(models.Model):
    role        = models.CharField(max_length=200)
    company     = models.CharField(max_length=200)
    duration    = models.CharField(max_length=100)
    description = models.TextField()
    image       = models.ImageField(upload_to='internships/', blank=True, null=True)
    order       = models.IntegerField(default=0)

    class Meta:
        db_table  = 'internship'
        app_label = 'portfolio'
        ordering  = ['order']


class Certification(models.Model):
    name           = models.CharField(max_length=200)
    issuer         = models.CharField(max_length=200)
    year           = models.CharField(max_length=10, blank=True, default='')
    description    = models.TextField(blank=True, default='')
    credential_url = models.URLField(blank=True, default='')
    image          = models.ImageField(upload_to='certs/', blank=True, null=True)
    order          = models.IntegerField(default=0)

    class Meta:
        db_table  = 'certification'
        app_label = 'portfolio'
        ordering  = ['order']


class Workshop(models.Model):
    title       = models.CharField(max_length=200)
    organizer   = models.CharField(max_length=200)
    date        = models.CharField(max_length=100)
    description = models.TextField()
    image       = models.ImageField(upload_to='workshops/', blank=True, null=True)
    image2      = models.ImageField(upload_to='workshops/', blank=True, null=True)
    image3      = models.ImageField(upload_to='workshops/', blank=True, null=True)
    order       = models.IntegerField(default=0)

    class Meta:
        db_table  = 'workshop'
        app_label = 'portfolio'
        ordering  = ['order']


class ContactMessage(models.Model):
    name    = models.CharField(max_length=100)
    email   = models.EmailField()
    message = models.TextField()
    sent_at = models.DateTimeField(auto_now_add=True)
    is_read = models.BooleanField(default=False)

    class Meta:
        db_table  = 'contact_message'
        app_label = 'portfolio'
        ordering  = ['-sent_at']


class GalleryImage(models.Model):
    CATEGORY_CHOICES = [
        ('certificate', 'Certificate'),
        ('internship',  'Internship'),
        ('workshop',    'Workshop'),
        ('project',     'Project'),
        ('achievement', 'Achievement'),
    ]
    title    = models.CharField(max_length=200)
    category = models.CharField(max_length=50, choices=CATEGORY_CHOICES)
    image    = models.ImageField(upload_to='gallery/')
    caption  = models.CharField(max_length=300, blank=True, default='')
    order    = models.IntegerField(default=0)

    class Meta:
        db_table  = 'gallery_image'
        app_label = 'portfolio'
        ordering  = ['category', 'order']
