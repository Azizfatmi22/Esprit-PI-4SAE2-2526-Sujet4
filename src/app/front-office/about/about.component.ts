import { Component } from '@angular/core';

@Component({
  selector: 'app-about',
  templateUrl: './about.component.html',
  styleUrl: './about.component.scss'
})
export class AboutComponent {
     // Statistiques de l'entreprise
  stats = [
    { number: '10+', label: 'Années d\'expertise', icon: '📅' },
    { number: '50,000+', label: 'Apprenants certifiés', icon: '🎓' },
    { number: '500+', label: 'Entreprises partenaires', icon: '🤝' },
    { number: '100+', label: 'Experts formateurs', icon: '👨‍🏫' }
  ];

  // Valeurs de l'entreprise
  values = [
    {
      icon: '🌟',
      title: 'Excellence',
      description: 'We strive for excellence in every certification and training program we offer.'
    },
    {
      icon: '🤝',
      title: 'Integrity',
      description: 'We act with transparency and honesty in all our interactions with learners and partners.'
    },
    {
      icon: '💡',
      title: 'Innovation',
      description: 'We constantly innovate to offer certification programs adapted to market needs.'
    },
    {
      icon: '🌍',
      title: 'Accessibility',
      description: 'We make quality education accessible to everyone, everywhere in the world.'
    }
  ];

  // Timeline - Company History
  timeline = [
    {
      year: '2014',
      title: 'Foundation of Formini',
      description: 'Creation of Formini with the mission to democratize access to professional certifications.',
      icon: '🚀'
    },
    {
      year: '2016',
      title: 'First major partnership',
      description: 'Signing of the first partnership with a major technology company.',
      icon: '🤝'
    },
    {
      year: '2018',
      title: 'Platform launch',
      description: 'Deployment of our innovative online learning platform.',
      icon: '💻'
    },
    {
      year: '2020',
      title: '50,000 learners',
      description: 'Reaching the milestone of 50,000 certified learners worldwide.',
      icon: '🎯'
    },
    {
      year: '2022',
      title: 'International expansion',
      description: 'Opening of offices in Europe and North America.',
      icon: '🌍'
    },
    {
      year: '2024',
      title: 'Market leader',
      description: 'Recognized as the leader in professional certifications in Africa.',
      icon: '🏆'
    }
  ];

  // Team - with pre-calculated initials
  team = [
    {
      name: 'Omar Aguil',
      role: 'Founder & CEO',
      bio: 'Former technical director with 15 years of experience in technology education.',
      image: 'assets/images/team/omar.jpg',
      initials: 'OA',
      linkedin: '#',
      twitter: '#'
    },
    {
      name: 'Kthiri Med Youssef',
      role: 'Academic Director',
      bio: 'PhD in educational sciences, passionate about pedagogical innovation.',
      image: 'assets/images/team/youssef.jpg',
      initials: 'KY',
      linkedin: '#',
      twitter: '#'
    },
    {
      name: 'Mehdi Trabelsi',
      role: 'CTO',
      bio: 'Expert in cloud architecture and artificial intelligence.',
      image: 'assets/images/team/mehdi.jpg',
      initials: 'MT',
      linkedin: '#',
      twitter: '#'
    },
    {
      name: 'Nour Ben Mahmoud',
      role: 'Partnership Director',
      bio: 'Develops relationships with companies and academic institutions.',
      image: 'assets/images/team/nour.jpg',
      initials: 'NB',
      linkedin: '#',
      twitter: '#'
    }
  ];

  // Testimonials
  testimonials = [
    {
      name: 'Ahmed Karim',
      role: 'CTO, Sofrecom Tunisia',
      content: 'Formini has transformed our approach to internal training. Their certifications are recognized and appreciated by our teams.',
      avatar: 'assets/images/testimonials/ahmed.jpg',
      initial: 'A',
      rating: 5
    },
    {
      name: 'Leila Mansour',
      role: 'HR Director, Vermeg',
      content: 'The quality of the programs and the professionalism of the trainers make Formini a trusted partner for our employees.',
      avatar: 'assets/images/testimonials/leila.jpg',
      initial: 'L',
      rating: 5
    },
    {
      name: 'Mohamed Ali Ben Salem',
      role: 'Certified Developer',
      content: 'Thanks to Formini, I obtained my AWS certification and landed a position as Cloud Architect at EY.',
      avatar: 'assets/images/testimonials/mohamed.jpg',
      initial: 'M',
      rating: 5
    }
  ];

  // Certifications and Accreditations
  accreditations = [
    { name: 'ISO 9001', icon: '✅' },
    { name: 'Qualiopi', icon: '🎯' },
    { name: 'Datadock', icon: '📊' },
    { name: 'OPQF', icon: '📜' }
  ];

  getStars(rating: number): number[] {
    return Array(rating).fill(1);
  }
}
