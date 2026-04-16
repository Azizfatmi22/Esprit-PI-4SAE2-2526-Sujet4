import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  features = [
    {
      icon: '🎓',
      title: 'Certifications Professionnelles',
      description:
        'Obtenez des certifications reconnues par les entreprises pour booster votre carrière.',
    },
    {
      icon: '📚',
      title: 'Cours Spécialisés',
      description:
        "Accédez à des cours créés par des experts de l'industrie dans divers domaines.",
    },
    {
      icon: '🎯',
      title: 'Évaluations Rigoureuses',
      description:
        'Passez des examens et quiz conçus pour valider vos compétences réelles.',
    },
    {
      icon: '📈',
      title: 'Suivi de Progression',
      description:
        'Visualisez votre évolution et obtenez des recommandations personnalisées.',
    },
  ];

  certifications = [
    {
      title: 'Développement Full Stack',
      category: 'Technologie',
      duration: '6 mois',
      level: 'Avancé',
    },
    {
      title: 'Data Science & IA',
      category: 'Analyse de données',
      duration: '8 mois',
      level: 'Expert',
    },
    {
      title: 'Cybersécurité',
      category: 'Sécurité informatique',
      duration: '4 mois',
      level: 'Intermédiaire',
    },
    {
      title: 'Cloud Computing',
      category: 'Infrastructure',
      duration: '5 mois',
      level: 'Avancé',
    },
  ];

  stats = [
    { number: '10,000+', label: 'Certifications délivrées' },
    { number: '500+', label: 'Entreprises partenaires' },
    { number: '98%', label: 'Taux de satisfaction' },
    { number: '50+', label: 'Pays desservis' },
  ];

  constructor() {}

  scrollToSection(sectionId: string) {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
