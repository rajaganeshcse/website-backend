import os
import django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from django.contrib.auth.models import User
from apps.portfolio.models import (
    Hero, Skill, Project, Education, Internship, Certification, Workshop
)

if not User.objects.filter(username='Neelamohan').exists():
    User.objects.create_superuser(
        username='Neelamohan',
        password='1234',
        email='neelamohan6cs@gmail.com'
    )
    print('Admin created  →  Neelamohan / 1234')
else:
    print('Admin already exists')

if not Hero.objects.exists():
    Hero.objects.create(
        name      = 'Neelamohan R',
        title     = 'Developer & Data Enthusiast',
        bio       = 'A passionate CSE student who enjoys building web applications and working with data. Hands-on experience with Python, SQL, Flask, React, and MongoDB. I enjoy turning ideas into functional, user-friendly products and always looking to grow and collaborate.',
        email     = 'neelamohan6cs@gmail.com',
        phone     = '+91 9500205859',
        github    = 'https://github.com/Neelamohan6cs',
        linkedin  = 'https://www.linkedin.com/in/neelamohan-r-cs',
        portfolio = 'https://neelamohan-profile.vercel.app/',
    )
    print('Hero created')

if not Skill.objects.exists():
    skills = [
        ('programming', 'Python',                90, 1),
        ('programming', 'JavaScript',            85, 2),
        ('programming', 'SQL',                   80, 3),
        ('frontend',    'React.js',              85, 1),
        ('frontend',    'HTML & CSS',            90, 2),
        ('backend',     'Flask',                 80, 1),
        ('backend',     'Node.js',               75, 2),
        ('backend',     'REST API',              85, 3),
        ('database',    'MongoDB',               82, 1),
        ('database',    'MySQL',                 78, 2),
        ('analytics',   'Data Analysis',         85, 1),
        ('analytics',   'Data Visualization',    80, 2),
        ('analytics',   'Tableau',               75, 3),
        ('ml',          'Supervised Learning',   80, 1),
        ('ml',          'Unsupervised Learning', 75, 2),
        ('ml',          'Model Evaluation',      80, 3),
        ('tools',       'Git & GitHub',          90, 1),
        ('tools',       'Postman',               85, 2),
        ('tools',       'AWS Fundamentals',      70, 3),
    ]
    for cat, name, level, order in skills:
        Skill.objects.create(category=cat, name=name, level=level, order=order)
    print('Skills created')

if not Project.objects.exists():
    Project.objects.create(
        title       = 'Smart Health Care Monitoring System',
        description = 'A smart rural health monitoring system that collects real-time vitals (temperature, pulse, heart rate) using IoT sensors and predicts health status using a machine learning model. Built a Flask backend for ML prediction, a React frontend for real-time visualization, and MongoDB for patient data storage. Designed for farmers and rural communities to enable early health risk detection.',
        tech_stack  = 'Python, Flask, React.js, Machine Learning, MongoDB, IoT, NodeMCU',
        github_url  = 'https://github.com/Neelamohan6cs/HealthCare-Project',
        order       = 1,
    )
    Project.objects.create(
        title       = 'Cattle Feed E-Commerce & Management Platform',
        description = 'A full-stack MERN-based e-commerce platform for selling cattle feed products such as Kambu, Cholam, and Pellets. Built a responsive React frontend and implemented secure REST APIs using Node.js and Express.js for authentication, product and order management. MongoDB for data storage and inventory handling.',
        tech_stack  = 'MongoDB, Express.js, React.js, Node.js, JavaScript, HTML, CSS, REST APIs',
        github_url  = 'https://github.com/Neelamohan6cs/dairy-products',
        order       = 2,
    )
    print('Projects created')

if not Education.objects.exists():
    Education.objects.create(
        degree      = 'B.E Computer Science & Engineering',
        institution = 'Dhanalakshmi Srinivasan Engineering College, Perambalur (Anna University)',
        year        = '2023 – 2027',
        score       = 'CGPA: 7.54',
        order       = 1,
    )
    Education.objects.create(
        degree      = 'HSC & SSLC',
        institution = 'Thanthai Hans Roever Higher Secondary School, Perambalur',
        year        = '2022 – 2023',
        score       = '78.59%',
        order       = 2,
    )
    print('Education created')

if not Internship.objects.exists():
    Internship.objects.create(
        role        = 'Data Analytics Intern',
        company     = 'Novitech R&D Private Limited, Coimbatore',
        duration    = 'AUG – SEP 2024',
        description = 'Gained hands-on experience in Python and SQL for data analysis. Worked with Excel and tables to clean, process, and visualize datasets. Explored data-driven insights to support company research. Applied analytical skills to solve real-world business problems.',
        order       = 1,
    )
    Internship.objects.create(
        role        = 'MERN Stack Intern',
        company     = 'Main Flow Services Techno Pvt. Ltd',
        duration    = 'JULY – AUG 2025',
        description = 'Built full-stack web apps using MongoDB, Express, React.js, and Node.js. Developed CRUD operations, REST APIs, and interactive frontend features. Explored authentication, MongoDB integration, and deployed apps on AWS EC2.',
        order       = 2,
    )
    print('Internships created')

if not Certification.objects.exists():
    certs = [
        ('Python Programming',             'GUVI – IIT Madras Endorsed',             '2024', 1),
        ('AWS Academy Cloud Foundations',  'AWS Academy',                             '2024', 2),
        ('Learnathon 2024',                'ICT Academy Chennai (SQL, Deep Learning)', '2024', 3),
        ('Data Analytics',                 'IBM',                                     '2025', 4),
        ('Certified MERN Stack Developer', 'Main Flow Technologies (AICTE)',           '',     5),
        ('MongoDB Certified',              'ICT Academy',                             '2026', 6),
    ]
    for name, issuer, year, order in certs:
        Certification.objects.create(name=name, issuer=issuer, year=year, order=order)
    print('Certifications created')

if not Workshop.objects.exists():
    Workshop.objects.create(
        title       = 'Artificial Intelligence with Machine Learning',
        organizer   = 'NIT Trichy',
        date        = '29 – 30 March 2025 (2 Days)',
        description = 'Hands-on workshop on AI, Machine Learning, and Data Analysis enhancing technical and problem-solving skills.',
        order       = 1,
    )
    Workshop.objects.create(
        title       = 'MongoDB Workshop – Campus Student Submission',
        organizer   = 'ICT Academy & Sairam Engineering College, Chennai',
        date        = '28 January 2026',
        description = 'Participated in a hands-on MongoDB workshop learning database management and gaining exposure to industry practices.',
        order       = 2,
    )
    print('Workshops created')

print('\nAll done!')
print('Now run: python manage.py runserver')
