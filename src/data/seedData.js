function withIds(items, prefix) {
  return items.map((item, index) => ({
    id: `${prefix}-${index + 1}`,
    ...item,
  }));
}

function splitTech(value) {
  return String(value || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

const heroSeed = {
  name: "Rajaganesh T",
  title: "Aspiring Android App Developer",
  bio: "A passionate and detail-oriented developer seeking an entry-level role in Android and mobile application development. Skilled in Java, Dart, Android Studio, Firebase, React, and problem solving, with hands-on experience building mobile apps and practical software projects.",
  email: "rajaganeshcse2005@gmail.com",
  phone: "+91 6382641748",
  github: "https://github.com/rajaganeshcse",
  linkedin: "https://www.linkedin.com/in/rajaganesh-t-835a21364",
  leetcode: "",
  instagram: "",
  portfolio: "https://react-website-five-theta.vercel.app/",
  location: "Vanrasankuppam, Cuddalore, Tamil Nadu, India",
  college: "Dhanalakshmi Srinivasan Engineering College, Perambalur - Computer Science and Engineering",
  address: "70, MaariAmman Kovil Street, Vanrasankuppam, Cuddalore (TK), Cuddalore (DT) - 607102",
  photo_path: "/media/photos/profile1.jpg",
  resume_path: "/media/resumes/rajaganesh-resume.pdf",
  resume_name: "Rajaganesh Resume (2).pdf",
};

const skillSeed = [
  { category: "programming", name: "Core Java", level: 90, order: 1 },
  { category: "programming", name: "Python", level: 78, order: 2 },
  { category: "frontend", name: "HTML", level: 85, order: 1 },
  { category: "frontend", name: "CSS", level: 82, order: 2 },
  { category: "frontend", name: "React", level: 80, order: 3 },
  { category: "mobile", name: "Android Studio", level: 88, order: 1 },
  { category: "mobile", name: "Firebase", level: 82, order: 2 },
  { category: "backend", name: "Flask", level: 72, order: 1 },
  { category: "database", name: "MySQL", level: 78, order: 1 },
  { category: "database", name: "MongoDB", level: 75, order: 2 },
  { category: "analytics", name: "Excel", level: 80, order: 1 },
  { category: "ml", name: "Machine Learning Basics", level: 72, order: 1 },
  { category: "tools", name: "Git", level: 84, order: 1 },
  { category: "tools", name: "GitHub", level: 84, order: 2 },
  { category: "tools", name: "Postman", level: 76, order: 3 },
  { category: "tools", name: "AWS Fundamentals", level: 65, order: 4 },
];

const projectSeed = [
  {
    title: "Smart Health Care Monitoring System",
    description:
      "Developed a smart rural health monitoring system that collects real-time vitals such as temperature, pulse, and heart rate using IoT sensors and predicts health status using a machine learning model. Built a Flask backend for prediction and API integration, a React frontend for real-time visualization, and used MongoDB for patient data storage.",
    tech_stack: "Python, Flask, React, MongoDB, Machine Learning, IoT, NodeMCU",
    github_url: "https://github.com/rajaganeshcse/smart_HealthCare_project.git",
    live_url: "",
    video_url: "",
    image_paths: [],
    order: 1,
  },
];

const appSeed = [
  {
    title: "Game Tournament App",
    description:
      "Designed and developed an Android application that enables users to join and manage online gaming tournaments. Implemented user authentication, tournament registration, and real-time data storage using Firebase Authentication and Firestore, with an intuitive UI built in Android Studio.",
    dashboard_url: "",
    github_url: "https://github.com/rajaganeshcse/MY_APP.git",
    playstore_url: "",
    cover_image_path: "",
    screenshot_paths: [],
    apk_path: "",
    apk_name: "",
    order: 1,
  },
];

const educationSeed = [
  {
    degree: "B.E Computer Science and Engineering",
    institution: "Dhanalakshmi Srinivasan Engineering College, Perambalur",
    year: "2023 - Present",
    score: "GPA: 8.86 / 10",
    result_pdf_path: "",
    result_pdf_name: "",
    documents: [],
    order: 1,
  },
  {
    degree: "Higher Secondary Education",
    institution: "Government Higher Secondary School, Naduveerapattu",
    year: "2021 - 2023",
    score: "Percentage: 76%",
    result_pdf_path: "",
    result_pdf_name: "",
    documents: [],
    order: 2,
  },
  {
    degree: "Secondary School Education",
    institution: "Government Higher Secondary School, Naduveerapattu",
    year: "2020 - 2021",
    score: "Percentage: 62%",
    result_pdf_path: "",
    result_pdf_name: "",
    documents: [],
    order: 3,
  },
];

const internshipSeed = [];

const certificationSeed = [
  { name: "SWAYAM - Cyber Security And Privacy", issuer: "SWAYAM", year: "", description: "", credential_url: "", image_path: "", order: 1 },
  { name: "Learnathon 2024", issuer: "ICT Academy", year: "2024", description: "", credential_url: "", image_path: "", order: 2 },
  { name: "MS Excel AI and Automation", issuer: "", year: "", description: "", credential_url: "", image_path: "", order: 3 },
  { name: "Java Spring Boot and App Development", issuer: "", year: "", description: "", credential_url: "", image_path: "", order: 4 },
  { name: "Flutter App Development (Dart)", issuer: "", year: "", description: "", credential_url: "", image_path: "", order: 5 },
  { name: "Microsoft Azure AI", issuer: "Microsoft", year: "", description: "", credential_url: "", image_path: "", order: 6 },
];

const workshopSeed = [
  {
    title: "Artificial Intelligence with Machine Learning",
    organizer: "NIT Trichy",
    date: "29.03.2025 - 30.03.2025 (2 days)",
    description: "Hands-on workshop on AI, machine learning, and data analysis, improving technical and problem-solving skills.",
    link_url: "",
    image_paths: [],
    order: 1,
  },
  {
    title: "Campus Student Submission - MongoDB Workshop - 2026",
    organizer: "ICT Academy, Chennai and Sairam Engineering College",
    date: "28.01.2026",
    description: "Participated in a hands-on MongoDB workshop, learning database management and gaining exposure to industry practices.",
    link_url: "",
    image_paths: [],
    order: 2,
  },
];

const journalSeed = [];

const gallerySeed = [
  {
    title: "Workshop Participation",
    category: "workshop",
    caption: "Hands-on technical participation and learning.",
    image_path: "/media/gallery/WhatsApp_Image_2026-03-14_at_6.13.00_PM.jpeg",
    order: 1,
  },
  {
    title: "Certificate Moment",
    category: "certificate",
    caption: "Academic and technical milestones.",
    image_path: "/media/gallery/WhatsApp_Image_2026-03-14_at_6.13.00_PM_1.jpeg",
    order: 2,
  },
  {
    title: "Achievement Snapshot",
    category: "achievement",
    caption: "Portfolio highlights and participation records.",
    image_path: "/media/gallery/WhatsApp_Image_2026-03-14_at_6.13.02_PM.jpeg",
    order: 3,
  },
];

function buildFallbackPortfolio(req, absoluteUrl) {
  return {
    hero: {
      id: "hero-1",
      name: heroSeed.name,
      title: heroSeed.title,
      bio: heroSeed.bio,
      email: heroSeed.email,
      phone: heroSeed.phone,
      github: heroSeed.github,
      linkedin: heroSeed.linkedin,
      leetcode: heroSeed.leetcode,
      instagram: heroSeed.instagram,
      portfolio: heroSeed.portfolio,
      location: heroSeed.location,
      college: heroSeed.college,
      address: heroSeed.address,
      photo_url: absoluteUrl(req, heroSeed.photo_path),
      resume_url: absoluteUrl(req, heroSeed.resume_path),
      resume_name: heroSeed.resume_name,
    },
    skills: withIds(skillSeed, "skill"),
    projects: withIds(projectSeed, "project").map((item) => {
      const imageUrls = (item.image_paths || []).map((value) => absoluteUrl(req, value)).filter(Boolean);
      return {
        id: item.id,
        title: item.title,
        description: item.description,
        tech_stack: item.tech_stack,
        tech_list: splitTech(item.tech_stack),
        github_url: item.github_url,
        live_url: item.live_url,
        video_url: item.video_url,
        image_urls: imageUrls,
        image_url: imageUrls[0] || "",
        image_url2: imageUrls[1] || "",
        image_url3: imageUrls[2] || "",
        order: item.order,
      };
    }),
    apps: withIds(appSeed, "app").map((item) => {
      const screenshotUrls = (item.screenshot_paths || []).map((value) => absoluteUrl(req, value)).filter(Boolean);
      return {
        id: item.id,
        title: item.title,
        description: item.description,
        dashboard_url: item.dashboard_url,
        github_url: item.github_url,
        playstore_url: item.playstore_url,
        cover_image_url: absoluteUrl(req, item.cover_image_path),
        screenshot_urls: screenshotUrls,
        screenshot_url1: screenshotUrls[0] || "",
        screenshot_url2: screenshotUrls[1] || "",
        screenshot_url3: screenshotUrls[2] || "",
        apk_url: absoluteUrl(req, item.apk_path),
        apk_name: item.apk_name,
        order: item.order,
      };
    }),
    education: withIds(educationSeed, "education").map((item) => ({
      id: item.id,
      degree: item.degree,
      institution: item.institution,
      year: item.year,
      score: item.score,
      documents: (item.documents || []).map((document, index) => ({
        id: `${item.id}-doc-${index + 1}`,
        title: document.title || document.pdf_name || `Document ${index + 1}`,
        pdf_url: absoluteUrl(req, document.pdf_path),
        pdf_download_url: absoluteUrl(req, document.pdf_path),
        pdf_name: document.pdf_name || "",
      })),
      result_pdf_url: absoluteUrl(req, item.result_pdf_path),
      result_pdf_download_url: absoluteUrl(req, item.result_pdf_path),
      result_pdf_name: item.result_pdf_name,
      order: item.order,
    })),
    internships: withIds(internshipSeed, "internship"),
    certifications: withIds(certificationSeed, "cert").map((item) => ({
      id: item.id,
      name: item.name,
      issuer: item.issuer,
      year: item.year,
      description: item.description,
      credential_url: item.credential_url,
      image_url: absoluteUrl(req, item.image_path),
      order: item.order,
    })),
    workshops: withIds(workshopSeed, "workshop").map((item) => {
      const imageUrls = (item.image_paths || []).map((value) => absoluteUrl(req, value)).filter(Boolean);
      return {
        id: item.id,
        title: item.title,
        organizer: item.organizer,
        date: item.date,
        description: item.description,
        link_url: item.link_url,
        image_urls: imageUrls,
        image_url: imageUrls[0] || "",
        image_url2: imageUrls[1] || "",
        image_url3: imageUrls[2] || "",
        order: item.order,
      };
    }),
    journals: withIds(journalSeed, "journal").map((item) => ({
      id: item.id,
      title: item.title,
      details: item.details,
      pdf_url: absoluteUrl(req, item.pdf_path),
      pdf_download_url: absoluteUrl(req, item.pdf_path),
      pdf_name: item.pdf_name || "",
      order: item.order,
    })),
  };
}

function buildFallbackGallery(req, absoluteUrl) {
  return withIds(gallerySeed, "gallery").map((item) => ({
    id: item.id,
    title: item.title,
    category: item.category,
    caption: item.caption,
    image_url: absoluteUrl(req, item.image_path),
    order: item.order,
  }));
}

function buildFallbackMessages() {
  return [];
}

module.exports = {
  heroSeed,
  skillSeed,
  projectSeed,
  appSeed,
  educationSeed,
  internshipSeed,
  certificationSeed,
  workshopSeed,
  journalSeed,
  gallerySeed,
  buildFallbackPortfolio,
  buildFallbackGallery,
  buildFallbackMessages,
};
