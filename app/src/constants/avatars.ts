export interface Avatar {
  id: string;
  emoji: string;
  name: string;
}

export const PREDEFINED_AVATARS: Avatar[] = [
  { id: 'dog', emoji: '🐶', name: 'Hund' },
  { id: 'cat', emoji: '🐱', name: 'Katze' },
  { id: 'rabbit', emoji: '🐰', name: 'Hase' },
  { id: 'fox', emoji: '🦊', name: 'Fuchs' },
  { id: 'bear', emoji: '🐻', name: 'Bär' },
  { id: 'panda', emoji: '🐼', name: 'Panda' },
  { id: 'koala', emoji: '🐨', name: 'Koala' },
  { id: 'tiger', emoji: '🐯', name: 'Tiger' },
  { id: 'lion', emoji: '🦁', name: 'Löwe' },
  { id: 'cow', emoji: '🐮', name: 'Kuh' },
  { id: 'pig', emoji: '🐷', name: 'Schwein' },
  { id: 'frog', emoji: '🐸', name: 'Frosch' },
  { id: 'monkey', emoji: '🐵', name: 'Affe' },
  { id: 'chicken', emoji: '🐔', name: 'Huhn' },
  { id: 'penguin', emoji: '🐧', name: 'Pinguin' },
  { id: 'bird', emoji: '🐦', name: 'Vogel' },
  { id: 'owl', emoji: '🦉', name: 'Eule' },
  { id: 'unicorn', emoji: '🦄', name: 'Einhorn' },
  { id: 'turtle', emoji: '🐢', name: 'Schildkröte' },
  { id: 'dolphin', emoji: '🐬', name: 'Delfin' },
];
