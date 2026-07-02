import re

def update_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # We need to replace specific widgets and remove 'const' where Theme.of(context) is introduced.
    # 1. _GlassCard
    content = content.replace(
        'color: Colors.white.withOpacity(0.15),', 
        'color: Theme.of(context).colorScheme.onSurface.withOpacity(0.05),'
    )
    content = content.replace(
        'border: Border.all(color: Colors.white.withOpacity(0.3)),',
        'border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1)),'
    )
    
    # 2. _PinIndicator
    content = content.replace(
        '(isFilled ? const Color(0xFF53A835) : Colors.white.withOpacity(0.5))',
        '(isFilled ? Theme.of(context).colorScheme.primary : Theme.of(context).colorScheme.onSurface.withOpacity(0.3))'
    )
    content = content.replace(
        'color = const Color(0xFF53A835);',
        'color = Theme.of(context).colorScheme.primary;'
    )

    # 3. _NumericKey
    content = content.replace(
        'color: Colors.white.withOpacity(0.1),',
        'color: Theme.of(context).colorScheme.onSurface.withOpacity(0.05),'
    )
    content = content.replace(
        'border: Border.all(color: Colors.white.withOpacity(0.2)),',
        'border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.1)),'
    )
    content = content.replace(
        'color: Colors.white, size: 28',
        'color: Theme.of(context).colorScheme.onSurface, size: 28'
    )
    content = content.replace(
        '''const TextStyle(
                        color: Colors.white,
                        fontSize: 32,
                        fontWeight: FontWeight.w400,
                      )''',
        '''TextStyle(
                        color: Theme.of(context).colorScheme.onSurface,
                        fontSize: 32,
                        fontWeight: FontWeight.w400,
                      )'''
    )

    # 4. _BiometricButton
    content = content.replace(
        'color: Colors.white.withOpacity(_isHovered ? 0.2 : 0.1),',
        'color: Theme.of(context).colorScheme.onSurface.withOpacity(_isHovered ? 0.1 : 0.05),'
    )
    content = content.replace(
        'color: Colors.white.withOpacity(0.4),',
        'color: Theme.of(context).colorScheme.onSurface.withOpacity(0.2),'
    )
    content = content.replace(
        'const Color(0xFF53A835).withOpacity(_isHovered ? 0.5 : 0.2),',
        'Theme.of(context).colorScheme.primary.withOpacity(_isHovered ? 0.5 : 0.2),'
    )
    content = content.replace(
        'color: Colors.white,',
        'color: Theme.of(context).colorScheme.onSurface,'
    )
    
    # 5. LoginPage - background gradient
    content = content.replace(
        '''gradient: LinearGradient(
                colors: [Color(0xFF637C5A), Color(0xFF415739)],
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
              ),''',
        '''gradient: LinearGradient(
                colors: Theme.of(context).brightness == Brightness.dark 
                    ? [const Color(0xFF637C5A), const Color(0xFF415739)]
                    : [const Color(0xFFC5E1A5), const Color(0xFFAED581)],
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
              ),'''
    )
    content = content.replace(
        '''const BoxDecoration(
              gradient: LinearGradient(''',
        '''BoxDecoration(
              gradient: LinearGradient('''
    )

    content = content.replace(
        'const Color(0xFF53A835)',
        'Theme.of(context).colorScheme.primary'
    )
    content = content.replace(
        'const Color(0xFFE65100)',
        'Colors.orange'
    )
    content = content.replace(
        'color: Colors.white70',
        'color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)'
    )
    content = content.replace(
        'color: Colors.white',
        'color: Theme.of(context).colorScheme.onSurface'
    )
    # Remove const where we injected Theme.of(context)
    content = content.replace('const Text(', 'Text(')
    content = content.replace('const TextStyle(', 'TextStyle(')
    content = content.replace('const Icon(', 'Icon(')

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

update_file(r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\auth\\presentation\\pages\\login_page.dart')
